import { DesignGallery } from './gallery/DesignGallery';
import { ToastProvider } from './design-system';

export default function App() {
  return (
    <ToastProvider>
      <DesignGallery />
    </ToastProvider>
  );
}
