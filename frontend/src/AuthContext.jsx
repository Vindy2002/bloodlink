import { createContext, useContext, useState } from 'react';

const Ctx = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('bl_user')); } catch { return null; }
  });

  const login = ({ token, user }) => {
    localStorage.setItem('bl_token', token);
    localStorage.setItem('bl_user', JSON.stringify(user));
    setUser(user);
  };
  const logout = () => {
    localStorage.removeItem('bl_token');
    localStorage.removeItem('bl_user');
    setUser(null);
  };
  return <Ctx.Provider value={{ user, login, logout }}>{children}</Ctx.Provider>;
}

export const useAuth = () => useContext(Ctx);
