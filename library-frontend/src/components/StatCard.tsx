import type { ReactNode } from "react"
import { Spinner } from "react-bootstrap"

type Tone = "primary" | "accent" | "success" | "danger" | "neutral"

interface StatCardProps {
    label: string
    value: ReactNode
    icon: string
    tone?: Tone
    hint?: ReactNode
    isLoading?: boolean
}

export default function StatCard({ label, value, icon, tone = "primary", hint, isLoading }: StatCardProps) {
    return (
        <div className="stat-card">
            <span className={`stat-icon tone-${tone}`}>
                <i className={`bi ${icon}`} />
            </span>
            <div className="stat-body">
                <span className="stat-label">{label}</span>
                <span className="stat-value">{isLoading ? <Spinner animation="border" size="sm" /> : value}</span>
                {hint && <span className="stat-hint">{hint}</span>}
            </div>
        </div>
    )
}
