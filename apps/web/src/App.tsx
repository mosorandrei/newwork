import React from 'react';
import { AuthProvider } from './core/auth/AuthContext';
import AppRouter from './core/router/AppRouter';

export default function App() {
    return (
        <AuthProvider>
            <AppRouter />
        </AuthProvider>
    );
}
