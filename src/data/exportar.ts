import type { DB } from './types';
import { informeMes, type InformeSoci } from './informes';
import { arrodonir, euros, nomComplet, nomMes } from './util';

function descarrega(blob: Blob, nom: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = nom;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 2000);
}

const dataDM = (ts: number) => new Date(ts).toLocaleDateString('ca-ES', { day: '2-digit', month: '2-digit' });

export async function excelJunta(db: DB, mes: string) {
  const { default: ExcelJS } = await import('exceljs');
  const informes = informeMes(db, mes).filter((i) => i.linies.length > 0);
  const wb = new ExcelJS.Workbook();
  wb.creator = db.config.nomSocietat;
  const r = wb.addWorksheet('Resum per soci');
  r.columns = [
    { header: 'Soci', key: 'soci', width: 30 },
    { header: 'Referència banc', key: 'ref', width: 16 },
    { header: 'Correu', key: 'email', width: 32 },
    { header: 'Consumicions', key: 'consum', width: 14 },
    { header: 'Àpats', key: 'apats', width: 12 },
    { header: 'Abonaments', key: 'abon', width: 13 },
    { header: 'Total', key: 'total', width: 12 },
  ];
  informes.forEach((i) =>
    r.addRow({
      soci: nomComplet(i.soci),
      ref: i.soci.refBanc,
      email: i.soci.email,
      consum: i.totalConsum,
      apats: i.totalApats,
      abon: i.totalAbonaments,
      total: i.total,
    }),
  );
  const n = informes.length + 1;
  const tr = r.addRow({ soci: 'Total', consum: { formula: `SUM(D2:D${n})` }, apats: { formula: `SUM(E2:E${n})` }, abon: { formula: `SUM(F2:F${n})` }, total: { formula: `SUM(G2:G${n})` } });
  tr.font = { bold: true };
  r.getRow(1).font = { bold: true, color: { argb: 'FFFFFFFF' } };
  r.getRow(1).fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF5E1A3A' } };
  ['D', 'E', 'F', 'G'].forEach((c) => (r.getColumn(c).numFmt = '#,##0.00 "€"'));
  r.views = [{ state: 'frozen', ySplit: 1 }];

  const d = wb.addWorksheet('Detall');
  d.columns = [
    { header: 'Data', key: 'data', width: 12 },
    { header: 'Soci', key: 'soci', width: 30 },
    { header: 'Concepte', key: 'concepte', width: 32 },
    { header: 'Detall', key: 'detall', width: 24 },
    { header: 'Quantitat', key: 'q', width: 10 },
    { header: 'Preu', key: 'preu', width: 10 },
    { header: 'Import', key: 'import', width: 11 },
  ];
  informes.forEach((i) =>
    i.linies.forEach((l) =>
      d.addRow({ data: new Date(l.ts), soci: nomComplet(i.soci), concepte: l.concepte, detall: l.detall, q: l.quantitat, preu: l.preu, import: l.import }),
    ),
  );
  d.getColumn('data').numFmt = 'dd/mm/yyyy';
  d.getColumn('preu').numFmt = '#,##0.00 "€"';
  d.getColumn('import').numFmt = '#,##0.00 "€"';
  d.getRow(1).font = { bold: true };
  d.autoFilter = { from: 'A1', to: 'G1' };
  d.views = [{ state: 'frozen', ySplit: 1 }];

  const prod = wb.addWorksheet('Per producte');
  prod.columns = [
    { header: 'Producte', key: 'p', width: 32 },
    { header: 'Unitats', key: 'u', width: 10 },
    { header: 'Import', key: 'i', width: 12 },
  ];
  const agg = new Map<string, { u: number; i: number }>();
  informes.forEach((inf) =>
    inf.linies.filter((l) => l.tipus === 'consum').forEach((l) => {
      const a = agg.get(l.concepte) ?? { u: 0, i: 0 };
      a.u += l.quantitat;
      a.i = arrodonir(a.i + l.import);
      agg.set(l.concepte, a);
    }),
  );
  [...agg.entries()].sort((a, b) => b[1].i - a[1].i).forEach(([p, v]) => prod.addRow({ p, u: v.u, i: v.i }));
  prod.getColumn('i').numFmt = '#,##0.00 "€"';
  prod.getRow(1).font = { bold: true };

  const buf = await wb.xlsx.writeBuffer();
  descarrega(new Blob([buf], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), `GastroBacus-consum-${mes}.xlsx`);
}

async function nouPdf() {
  const { jsPDF } = await import('jspdf');
  const { default: autoTable } = await import('jspdf-autotable');
  return { doc: new jsPDF({ unit: 'mm', format: 'a4' }), autoTable };
}

const MOST: [number, number, number] = [94, 26, 58];

function capcaleraPdf(doc: import('jspdf').jsPDF, titol: string, subtitol: string) {
  doc.setFillColor(...MOST);
  doc.rect(0, 0, 210, 28, 'F');
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(18);
  doc.text('GastroBacus', 14, 13);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11);
  doc.text('Societat gastronòmica', 14, 20);
  doc.setTextColor(43, 33, 24);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(15);
  doc.text(titol, 14, 40);
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11);
  doc.text(subtitol, 14, 47);
}

export async function pdfSoci(inf: InformeSoci, mes: string) {
  const { doc, autoTable } = await nouPdf();
  capcaleraPdf(doc, `Consum ${nomMes(mes).llarg}`, `${nomComplet(inf.soci)} · Ref. ${inf.soci.refBanc}`);
  autoTable(doc, {
    startY: 54,
    head: [['Data', 'Concepte', 'Quant.', 'Preu', 'Import']],
    body: inf.linies.map((l) => [dataDM(l.ts), l.concepte + (l.detall ? ` (${l.detall})` : ''), String(l.quantitat), euros(l.preu), euros(l.import)]),
    foot: [['', 'Total a carregar', '', '', euros(inf.total)]],
    headStyles: { fillColor: MOST },
    footStyles: { fillColor: [238, 240, 232], textColor: [43, 33, 24] },
    columnStyles: { 2: { halign: 'right' }, 3: { halign: 'right' }, 4: { halign: 'right' } },
    styles: { fontSize: 10 },
  });
  doc.save(`GastroBacus-${mes}-${inf.soci.nom}-${inf.soci.cognoms.split(' ')[0]}.pdf`);
}

export async function pdfJunta(db: DB, mes: string) {
  const informes = informeMes(db, mes).filter((i) => i.linies.length > 0);
  const { doc, autoTable } = await nouPdf();
  const total = arrodonir(informes.reduce((s, i) => s + i.total, 0));
  capcaleraPdf(doc, `Informe de consum ${nomMes(mes).llarg}`, `${informes.length} socis · Total ${euros(total)}`);
  autoTable(doc, {
    startY: 54,
    head: [['Soci', 'Ref. banc', 'Consum', 'Àpats', 'Abonaments', 'Total']],
    body: informes.map((i) => [nomComplet(i.soci), i.soci.refBanc, euros(i.totalConsum), euros(i.totalApats), euros(i.totalAbonaments), euros(i.total)]),
    foot: [['Total', '', '', '', '', euros(total)]],
    headStyles: { fillColor: MOST },
    footStyles: { fillColor: [238, 240, 232], textColor: [43, 33, 24] },
    columnStyles: { 2: { halign: 'right' }, 3: { halign: 'right' }, 4: { halign: 'right' }, 5: { halign: 'right' } },
    styles: { fontSize: 10 },
  });
  doc.save(`GastroBacus-informe-${mes}.pdf`);
}

/** Fitxer provisional per al banc: imports per soci. El format definitiu (SEPA XML o el del banc) està pendent. */
export function csvRemesa(db: DB, mes: string) {
  const informes = informeMes(db, mes).filter((i) => i.total > 0);
  const files = [['referencia_banc', 'nom', 'import', 'concepte']].concat(
    informes.map((i) => [i.soci.refBanc, nomComplet(i.soci), i.total.toFixed(2), `Consum GastroBacus ${mes}`]),
  );
  const csv = files.map((f) => f.map((c) => `"${c.replace(/"/g, '""')}"`).join(';')).join('\r\n');
  descarrega(new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' }), `GastroBacus-remesa-${mes}.csv`);
}
