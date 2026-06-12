import { RouterProvider } from 'react-router-dom';

// project imports
import router from 'routes';
import ThemeCustomization from 'themes';
import AuthBootstrap from 'stores/AuthBootstrap';

// ==============================|| APP - THEME, ROUTER, LOCAL ||============================== //

export default function App() {
  return (
    <ThemeCustomization>
      <AuthBootstrap />
      <RouterProvider router={router} />
    </ThemeCustomization>
  );
}
