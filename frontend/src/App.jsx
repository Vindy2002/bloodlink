import { Navigate, Route, Routes } from 'react-router-dom';
import { useAuth } from './AuthContext.jsx';
import Navbar from './components/Navbar.jsx';
import Home from './pages/Home.jsx';
import Search from './pages/Search.jsx';
import Login from './pages/Login.jsx';
import Register from './pages/Register.jsx';
import DonorDashboard from './pages/DonorDashboard.jsx';
import HospitalDashboard from './pages/HospitalDashboard.jsx';
import AdminDashboard from './pages/AdminDashboard.jsx';

function Protected({ role, children }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (user.role !== role) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/search" element={<Search />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/donor" element={<Protected role="DONOR"><DonorDashboard /></Protected>} />
        <Route path="/hospital" element={<Protected role="HOSPITAL"><HospitalDashboard /></Protected>} />
        <Route path="/admin" element={<Protected role="ADMIN"><AdminDashboard /></Protected>} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}
