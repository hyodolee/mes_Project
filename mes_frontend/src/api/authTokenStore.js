const MES_TOKEN_KEY = 'mesAccessToken';
const MCS_TOKEN_KEY = 'mcsAccessToken';

function getItem(key) {
  if (typeof window === 'undefined') {
    return null;
  }
  return window.localStorage.getItem(key);
}

function setItem(key, value) {
  if (typeof window === 'undefined') {
    return;
  }
  if (value) {
    window.localStorage.setItem(key, value);
  } else {
    window.localStorage.removeItem(key);
  }
}

export const authTokenStore = {
  getMesToken: () => getItem(MES_TOKEN_KEY),
  getMcsToken: () => getItem(MCS_TOKEN_KEY),
  setTokens: ({ mesToken, mcsToken }) => {
    setItem(MES_TOKEN_KEY, mesToken);
    setItem(MCS_TOKEN_KEY, mcsToken);
  },
  clear: () => {
    setItem(MES_TOKEN_KEY, null);
    setItem(MCS_TOKEN_KEY, null);
  },
  hasTokens: () => Boolean(getItem(MES_TOKEN_KEY) && getItem(MCS_TOKEN_KEY))
};
