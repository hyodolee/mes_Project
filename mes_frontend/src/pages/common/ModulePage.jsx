import PropTypes from 'prop-types';

import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Grid from '@mui/material/Grid';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import MainCard from 'components/MainCard';

export default function ModulePage({ title, description, status = '준비 중', metrics = [], tasks = [] }) {
  return (
    <Stack spacing={3}>
      <Box>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5} sx={{ alignItems: { xs: 'flex-start', sm: 'center' } }}>
          <Typography variant="h3">{title}</Typography>
          <Chip label={status} color="primary" size="small" variant="light" />
        </Stack>
        <Typography variant="body1" color="text.secondary" sx={{ mt: 1, maxWidth: 760 }}>
          {description}
        </Typography>
      </Box>

      <Grid container spacing={2.5}>
        {metrics.map((metric) => (
          <Grid key={metric.label} size={{ xs: 12, sm: 6, md: 3 }}>
            <MainCard>
              <Stack spacing={0.5}>
                <Typography variant="h4">{metric.value}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {metric.label}
                </Typography>
              </Stack>
            </MainCard>
          </Grid>
        ))}
      </Grid>

      <MainCard title="구현 범위">
        <Grid container spacing={2}>
          {tasks.map((task) => (
            <Grid key={task.title} size={{ xs: 12, md: 6 }}>
              <Stack spacing={0.75}>
                <Typography variant="subtitle1">{task.title}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {task.description}
                </Typography>
              </Stack>
            </Grid>
          ))}
        </Grid>
      </MainCard>
    </Stack>
  );
}

ModulePage.propTypes = {
  title: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  status: PropTypes.string,
  metrics: PropTypes.array,
  tasks: PropTypes.array
};
