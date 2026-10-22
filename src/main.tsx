import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import './index.css';
import App from './App';
import { DBProvider } from './data/store';
import { ToastProvider } from './components/comuns';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <DBProvider>
      <ToastProvider>
        <App />
      </ToastProvider>
    </DBProvider>
  </StrictMode>,
);

// Funcionament com a app instal·lable (fora de la vista prèvia)
if ('serviceWorker' in navigator && import.meta.env.PROD && import.meta.env.VITE_SENSE_SW !== '1') {
  import('virtual:pwa-register').then(({ registerSW }) => registerSW({ immediate: true })).catch(() => {});
}
