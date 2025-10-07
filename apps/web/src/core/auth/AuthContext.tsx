import React, { createContext, useContext, useState } from 'react';
import type {Session} from "../../api/auth.api.ts";
import {login} from "../../api/auth.api.ts";
import {setAuthToken} from "../../api/client.ts";


type AuthCtx = {
    session: Session | null;
    loginUser: (email: string, password: string) => Promise<void>;
    logout: () => void;
};
const AuthContext = createContext<AuthCtx>(null!);
export const useAuth = () => useContext(AuthContext);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [session, setSession] = useState<Session | null>(null);

    async function loginUser(email: string, password: string) {
        const s = await login(email, password);
        setSession(s);
    }

    function logout() {
        setSession(null);
        setAuthToken(null);
    }

    return <AuthContext.Provider value={{ session, loginUser, logout }}>{children}</AuthContext.Provider>;
};
