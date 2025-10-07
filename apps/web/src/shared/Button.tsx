import React from "react";

type Props = React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'ghost' };
export default function Button({ variant = 'primary', className = '', ...rest }: Props) {
    const base = 'btn';
    const style = variant === 'primary' ? 'btn-primary' : 'btn-ghost';
    return <button className={`${base} ${style} ${className}`} {...rest} />;
}
