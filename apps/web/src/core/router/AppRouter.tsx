import React from 'react';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';
import LoginPage from '../../features/auth/pages/LoginPage';
import DashboardPage from '../../features/dashboard/pages/DashboardPage';
import EmployeePage from '../../features/employee/pages/EmployeePage';

const router = createBrowserRouter([
    { path: '/', element: <DashboardPage /> },
    { path: '/login', element: <LoginPage /> },
    { path: '/employees/:id', element: <EmployeePage /> },
]);

export default function AppRouter() {
    return <RouterProvider router={router} />;
}
