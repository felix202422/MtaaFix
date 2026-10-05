import { Box } from '@mui/material';
import Sidebar from './Sidebar';
import Header from './Header';
import { SIDEBAR_WIDTH } from '../../utils/constants';

export default function Layout({ children, title }) {
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Sidebar />
      <Box sx={{ flexGrow: 1, ml: `${SIDEBAR_WIDTH}px`, bgcolor: 'background.default', minHeight: '100vh' }}>
        <Header title={title} />
        <Box sx={{ p: 3 }}>{children}</Box>
      </Box>
    </Box>
  );
}
