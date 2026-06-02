import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

export default function Footer() {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      sx={{ gap: 1.5, alignItems: 'center', justifyContent: 'space-between', p: '24px 16px 0px', mt: 'auto' }}
    >
      <Typography variant="caption">MES/MCS Operations Portfolio</Typography>
      <Stack direction="row" sx={{ gap: 1.5, alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="caption" color="text.secondary">
          MES 8080
        </Typography>
        <Typography variant="caption" color="text.secondary">
          MCS 8081
        </Typography>
        <Typography variant="caption" color="text.secondary">
          React Frontend
        </Typography>
      </Stack>
    </Stack>
  );
}
