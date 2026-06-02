import Box from '@mui/material/Box';

export default function LogoIcon() {
  return (
    <Box
      sx={(theme) => ({
        width: 34,
        height: 34,
        borderRadius: 1,
        display: 'grid',
        placeItems: 'center',
        color: theme.vars.palette.common.white,
        bgcolor: theme.vars.palette.primary.main,
        fontSize: 18,
        fontWeight: 800,
        lineHeight: 1
      })}
    >
      M
    </Box>
  );
}
