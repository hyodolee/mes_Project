import PropTypes from 'prop-types';
import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';

// material-ui
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import CircularProgress from '@mui/material/CircularProgress';
import FormHelperText from '@mui/material/FormHelperText';
import Grid from '@mui/material/Grid';
import InputAdornment from '@mui/material/InputAdornment';
import InputLabel from '@mui/material/InputLabel';
import OutlinedInput from '@mui/material/OutlinedInput';
import Stack from '@mui/material/Stack';

// third-party
import * as Yup from 'yup';
import { Formik } from 'formik';

// project imports
import IconButton from 'components/@extended/IconButton';
import AnimateButton from 'components/@extended/AnimateButton';
import { useAuthStore } from 'stores/authStore';

// assets
import EyeOutlined from '@ant-design/icons/EyeOutlined';
import EyeInvisibleOutlined from '@ant-design/icons/EyeInvisibleOutlined';

// ============================|| LOGIN FORM ||============================ //

export default function AuthLogin({ isDemo = false }) {
  const navigate = useNavigate();
  const location = useLocation();
  const login = useAuthStore((state) => state.login);
  const [showPassword, setShowPassword] = React.useState(false);

  const handleClickShowPassword = () => {
    setShowPassword((current) => !current);
  };

  const handleMouseDownPassword = (event) => {
    event.preventDefault();
  };

  return (
    <Formik
      initialValues={{
        username: 'admin',
        password: 'admin1234',
        submit: null
      }}
      validationSchema={Yup.object().shape({
        username: Yup.string().max(50, '아이디는 50자 이하로 입력하세요.').required('아이디를 입력하세요.'),
        password: Yup.string()
          .required('비밀번호를 입력하세요.')
          .test('no-leading-trailing-whitespace', '비밀번호 앞뒤에는 공백을 사용할 수 없습니다.', (value) => value === value.trim())
          .max(50, '비밀번호는 50자 이하로 입력하세요.')
      })}
      onSubmit={async (values, { setErrors, setSubmitting }) => {
        try {
          await login({ username: values.username, password: values.password });
          const from = location.state?.from?.pathname || '/';
          navigate(from, { replace: true });
        } catch (error) {
          setErrors({ submit: error.message || '로그인에 실패했습니다.' });
        } finally {
          setSubmitting(false);
        }
      }}
    >
      {({ errors, handleBlur, handleChange, handleSubmit, isSubmitting, touched, values }) => (
        <form noValidate onSubmit={handleSubmit}>
          <Grid container spacing={3}>
            {errors.submit && (
              <Grid size={12}>
                <Alert severity="error">{errors.submit}</Alert>
              </Grid>
            )}
            <Grid size={12}>
              <Stack sx={{ gap: 1 }}>
                <InputLabel htmlFor="username-login">아이디</InputLabel>
                <OutlinedInput
                  id="username-login"
                  type="text"
                  value={values.username}
                  name="username"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  placeholder="admin"
                  fullWidth
                  error={Boolean(touched.username && errors.username)}
                />
              </Stack>
              {touched.username && errors.username && (
                <FormHelperText error id="standard-weight-helper-text-username-login">
                  {errors.username}
                </FormHelperText>
              )}
            </Grid>
            <Grid size={12}>
              <Stack sx={{ gap: 1 }}>
                <InputLabel htmlFor="password-login">비밀번호</InputLabel>
                <OutlinedInput
                  fullWidth
                  error={Boolean(touched.password && errors.password)}
                  id="password-login"
                  type={showPassword ? 'text' : 'password'}
                  value={values.password}
                  name="password"
                  onBlur={handleBlur}
                  onChange={handleChange}
                  endAdornment={
                    <InputAdornment position="end">
                      <IconButton
                        aria-label="비밀번호 표시 전환"
                        onClick={handleClickShowPassword}
                        onMouseDown={handleMouseDownPassword}
                        edge="end"
                        color="secondary"
                      >
                        {showPassword ? <EyeOutlined /> : <EyeInvisibleOutlined />}
                      </IconButton>
                    </InputAdornment>
                  }
                  placeholder="admin1234"
                />
              </Stack>
              {touched.password && errors.password && (
                <FormHelperText error id="standard-weight-helper-text-password-login">
                  {errors.password}
                </FormHelperText>
              )}
            </Grid>
            <Grid sx={{ mt: -1 }} size={12}>
              <Alert severity="info" variant="outlined">
                개발 기본 계정: 관리자 admin / admin1234, 일반 사용자 user / user1234. 실제 값은 백엔드 환경변수로 변경할 수 있습니다.
              </Alert>
            </Grid>
            <Grid size={12}>
              <AnimateButton>
                <Button fullWidth size="large" type="submit" variant="contained" color="primary" disabled={isSubmitting}>
                  {isSubmitting && <CircularProgress size={18} color="inherit" sx={{ mr: 1 }} />}
                  로그인
                </Button>
              </AnimateButton>
            </Grid>
          </Grid>
        </form>
      )}
    </Formik>
  );
}

AuthLogin.propTypes = { isDemo: PropTypes.bool };
