import type { ReactNode } from "react"

interface PageHeaderProps {
    eyebrow: string
    title: string
    description?: ReactNode
    actions?: ReactNode
}

export default function PageHeader({ eyebrow, title, description, actions }: PageHeaderProps) {
    return (
        <header className="page-header">
            <div>
                <p className="page-eyebrow">{eyebrow}</p>
                <h1 className="page-title">{title}</h1>
                {description && <p className="page-description">{description}</p>}
            </div>
            {actions && <div className="page-actions">{actions}</div>}
        </header>
    )
}
