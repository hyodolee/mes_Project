import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

export default function LogoMain() {
  return (
    <Stack direction="row" spacing={1.25} sx={{ alignItems: 'center' }}>
      <Box
        sx={(theme) => ({
          width: 34,
          height: 34,
          borderRadius: 1,
          display: 'grid',
          placeItems: 'center',
          color: theme.vars.palette.common.white,
          bgcolor: theme.vars.palette.primary.main,
          fontWeight: 800,
          lineHeight: 1
        })}
      >
        M
      </Box>
      <Box>
        <Typography variant="subtitle1" sx={{ lineHeight: 1.1 }}>
          MES/MCS
        </Typography>
        <Typography variant="caption" color="text.secondary" sx={{ lineHeight: 1 }}>
          Operations
        </Typography>
      </Box>
    </Stack>
  );
}
