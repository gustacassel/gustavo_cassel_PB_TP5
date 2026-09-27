import { NavLink, Outlet } from "react-router-dom"
import { useServiceStatus, type ServiceState } from "../hooks/useServiceStatus"
import { useTheme } from "../hooks/useTheme"
import "./AppLayout.css"

const NAV_ITEMS = [
    { to: "/", label: "Início", icon: "bi-grid-1x2", end: true },
    { to: "/loans", label: "Empréstimos", icon: "bi-arrow-left-right" },
    { to: "/books", label: "Livros", icon: "bi-journal-bookmark" },
    { to: "/students", label: "Estudantes", icon: "bi-people" },
    { to: "/courses", label: "Cursos", icon: "bi-mortarboard" },
]

const STATE_LABELS: Record<ServiceState, string> = {
    checking: "verificando",
    online: "online",
    offline: "offline",
}

function ServiceIndicator({ name, route, state }: { name: string; route: string; state: ServiceState }) {
    return (
        <div className="service-indicator">
            <span className={`status-dot status-${state}`} />
            <div className="service-indicator-text">
                <span className="service-name">{name}</span>
                <span className="service-meta">
                    {route} · {STATE_LABELS[state]}
                </span>
            </div>
        </div>
    )
}

export default function AppLayout() {
    const status = useServiceStatus()
    const { theme, toggleTheme } = useTheme()

    return (
        <div className="app-shell">
            <aside className="app-sidebar">
                <div className="sidebar-brand">
                    <span className="brand-mark">
                        <i className="bi bi-book-half" />
                    </span>
                    <div>
                        <span className="brand-name">Biblioteca Aurora</span>
                        <span className="brand-tag">Projeto de Bloco · TP5</span>
                    </div>
                </div>

                <nav className="sidebar-nav">
                    {NAV_ITEMS.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.end}
                            className={({ isActive }) => `sidebar-link${isActive ? " active" : ""}`}
                        >
                            <i className={`bi ${item.icon}`} />
                            <span>{item.label}</span>
                        </NavLink>
                    ))}
                </nav>

                <button type="button" className="theme-toggle" onClick={toggleTheme}>
                    <i className={`bi ${theme === "dark" ? "bi-sun" : "bi-moon-stars"}`} />
                    <span>{theme === "dark" ? "Tema claro" : "Tema escuro"}</span>
                </button>

                <div className="sidebar-services">
                    <span className="sidebar-section-title">Serviços · via api-gateway</span>
                    <ServiceIndicator name="library-api" route="/library-api" state={status.library} />
                    <ServiceIndicator name="students-api" route="/students-api" state={status.students} />
                    <div className="service-indicator broker">
                        <i className="bi bi-broadcast" />
                        <div className="service-indicator-text">
                            <span className="service-name">RabbitMQ</span>
                            <span className="service-meta">students.events · library.events</span>
                        </div>
                    </div>
                </div>
            </aside>

            <main className="app-content">
                <Outlet />
            </main>
        </div>
    )
}
