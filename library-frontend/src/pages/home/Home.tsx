import { useCallback, useEffect, useState } from "react"
import { Badge, Button, Col, Row, Spinner, Table } from "react-bootstrap"
import { Link } from "react-router-dom"
import PageHeader from "../../components/PageHeader"
import StatCard from "../../components/StatCard"
import { getBooks } from "../../services/books-api"
import { getCourses } from "../../services/courses-api"
import { getStudentReplicaHealth, type StudentReplicaHealth } from "../../services/integration-api"
import { getLoans } from "../../services/loans-api"
import { getStudents } from "../../services/students-api"
import type { Loan } from "../../types/loans"
import { formatDate, formatDateTime, todayIso } from "../../utils/format"
import "./Home.css"

interface Dashboard {
    books: number | null
    loans: Loan[] | null
    students: number | null
    courses: number | null
    replica: StudentReplicaHealth | null
}

const EMPTY: Dashboard = { books: null, loans: null, students: null, courses: null, replica: null }

const valueOf = <T,>(result: PromiseSettledResult<T>) => (result.status === "fulfilled" ? result.value : null)

const FLOWS = [
    {
        from: "students-api",
        exchange: "students.events",
        to: "library-api",
        keys: ["student.created", "student.updated", "student.deleted"],
        effect: "mantém a cópia local de alunos usada para validar empréstimos",
    },
    {
        from: "library-api",
        exchange: "library.events",
        to: "students-api",
        keys: ["loan.created", "loan.returned", "loan.deleted"],
        effect: "conta os empréstimos ativos e impede excluir aluno com livro em mãos",
    },
]

export default function Home() {
    const [data, setData] = useState<Dashboard>(EMPTY)
    const [isLoading, setIsLoading] = useState(true)

    const fetchData = useCallback(async () => {
        // allSettled: um servico fora do ar nao zera o painel inteiro
        const [books, loans, students, courses, replica] = await Promise.allSettled([
            getBooks(),
            getLoans(),
            getStudents(),
            getCourses(),
            getStudentReplicaHealth(),
        ])
        setData({
            books: valueOf(books)?.length ?? null,
            loans: valueOf(loans),
            students: valueOf(students)?.length ?? null,
            courses: valueOf(courses)?.length ?? null,
            replica: valueOf(replica),
        })
        setIsLoading(false)
    }, [])

    const load = useCallback(async () => {
        setIsLoading(true)
        await fetchData()
    }, [fetchData])

    useEffect(() => {
        const run = async () => {
            await fetchData()
        }
        run()
    }, [fetchData])

    const activeLoans = data.loans?.filter((loan) => loan.status === "ACTIVE") ?? []
    const overdueLoans = activeLoans.filter((loan) => loan.dueDate < todayIso())
    const recentLoans = [...(data.loans ?? [])].sort((a, b) => b.id - a.id).slice(0, 5)

    const studentsApiDown = !isLoading && data.students === null
    const inSync = data.replica !== null && data.students !== null && data.replica.studentCount === data.students

    const syncBadge = () => {
        if (isLoading) {
            return <Spinner animation="border" size="sm" />
        }
        if (data.replica === null) {
            return <Badge bg="danger">library-api offline</Badge>
        }
        if (studentsApiDown) {
            return <Badge bg="warning" text="dark">students-api offline · usando a cópia local</Badge>
        }
        return inSync ? <Badge bg="success">sincronizado</Badge> : <Badge bg="info">aguardando eventos</Badge>
    }

    return (
        <>
            <PageHeader
                eyebrow="Painel"
                title="Visão geral da biblioteca"
                description="Arquitetura orientada a eventos: library-api e students-api não se chamam diretamente, trocam eventos pelo RabbitMQ."
                actions={
                    <>
                        <Button variant="outline-primary" onClick={load} disabled={isLoading}>
                            <i className="bi bi-arrow-clockwise me-2" />
                            Atualizar
                        </Button>
                        <Link to="/loans" className="btn btn-primary">
                            <i className="bi bi-plus-lg me-2" />
                            Novo empréstimo
                        </Link>
                    </>
                }
            />

            <div className="stat-grid">
                <StatCard label="Livros no acervo" value={data.books ?? "-"} icon="bi-journal-bookmark" isLoading={isLoading} />
                <StatCard
                    label="Empréstimos ativos"
                    value={data.loans ? activeLoans.length : "-"}
                    icon="bi-arrow-left-right"
                    tone="accent"
                    hint={overdueLoans.length > 0 ? `${overdueLoans.length} em atraso` : undefined}
                    isLoading={isLoading}
                />
                <StatCard
                    label="Estudantes"
                    value={data.students ?? data.replica?.studentCount ?? "-"}
                    icon="bi-people"
                    tone="success"
                    hint={studentsApiDown && data.replica ? "pela cópia local" : undefined}
                    isLoading={isLoading}
                />
                <StatCard label="Cursos" value={data.courses ?? "-"} icon="bi-mortarboard" tone="neutral" isLoading={isLoading} />
            </div>

            <Row className="g-4">
                <Col xxl={7}>
                    <section className="data-card h-100">
                        <div className="data-card-header">
                            <div>
                                <h2 className="data-card-title">Empréstimos recentes</h2>
                                <span className="data-card-subtitle">Últimos registros da library-api</span>
                            </div>
                            <Link to="/loans" className="btn btn-sm btn-outline-primary">
                                Ver todos
                            </Link>
                        </div>
                        <Table responsive className="data-table">
                            <thead>
                                <tr>
                                    <th>Livro</th>
                                    <th>Estudante</th>
                                    <th>Devolução</th>
                                    <th>Situação</th>
                                </tr>
                            </thead>
                            <tbody>
                                {recentLoans.map((loan) => (
                                    <tr key={loan.id}>
                                        <td>
                                            <span className="cell-main">{loan.bookTitle}</span>
                                            <span className="cell-sub">{loan.bookAuthor}</span>
                                        </td>
                                        <td>
                                            <span className="cell-main">{loan.studentName}</span>
                                            <span className="cell-sub">{loan.studentCourseName ?? "-"}</span>
                                        </td>
                                        <td>{formatDate(loan.returnDate ?? loan.dueDate)}</td>
                                        <td>
                                            {loan.status === "RETURNED" ? (
                                                <Badge bg="secondary">Devolvido</Badge>
                                            ) : loan.dueDate < todayIso() ? (
                                                <Badge bg="danger">Atrasado</Badge>
                                            ) : (
                                                <Badge bg="primary">Ativo</Badge>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                                {!isLoading && recentLoans.length === 0 && (
                                    <tr>
                                        <td colSpan={4}>
                                            <div className="empty-state">
                                                <i className="bi bi-inbox" />
                                                Nenhum empréstimo registrado ainda.
                                            </div>
                                        </td>
                                    </tr>
                                )}
                            </tbody>
                        </Table>
                    </section>
                </Col>

                <Col xxl={5}>
                    <section className="data-card h-100">
                        <div className="data-card-header">
                            <div>
                                <h2 className="data-card-title">Integração por eventos</h2>
                                <span className="data-card-subtitle">RabbitMQ · exchanges do tipo topic</span>
                            </div>
                            {syncBadge()}
                        </div>
                        <div className="data-card-body">
                            <div className="replica-summary">
                                <div>
                                    <span className="replica-label">Alunos na cópia local</span>
                                    <span className="replica-value">{data.replica?.studentCount ?? "-"}</span>
                                </div>
                                <div>
                                    <span className="replica-label">Último evento recebido</span>
                                    <span className="replica-value small-value">
                                        {formatDateTime(data.replica?.lastEventAt)}
                                    </span>
                                </div>
                            </div>

                            {FLOWS.map((flow) => (
                                <div className="event-flow" key={flow.exchange}>
                                    <div className="event-flow-line">
                                        <span className="flow-node">{flow.from}</span>
                                        <i className="bi bi-arrow-right flow-arrow" />
                                        <span className="flow-node exchange">
                                            <i className="bi bi-broadcast me-1" />
                                            {flow.exchange}
                                        </span>
                                        <i className="bi bi-arrow-right flow-arrow" />
                                        <span className="flow-node">{flow.to}</span>
                                    </div>
                                    <div className="flow-keys">
                                        {flow.keys.map((key) => (
                                            <code key={key}>{key}</code>
                                        ))}
                                    </div>
                                    <p className="flow-effect">{flow.effect}</p>
                                </div>
                            ))}
                        </div>
                    </section>
                </Col>
            </Row>
        </>
    )
}
