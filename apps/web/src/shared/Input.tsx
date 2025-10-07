import React from "react";

export default function Input(props: React.InputHTMLAttributes<HTMLInputElement>) {
    return <input className="form-input" {...props} />;
}
