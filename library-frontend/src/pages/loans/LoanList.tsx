import { useCallback, useEffect, useMemo, useState } from "react"
import { Badge, Button, Spinner, Table } from "react-bootstrap"
import Swal from "sweetalert2"
import PageHeader from "../../components/PageHeader"
import StatCard from "../../components/StatCard"
import { getBooks } from "../../services/books-api"
import { getReplicatedStudents, type ReplicatedStudent } from "../../services/integration-api"
import { createLoan, deleteLoan, getLoans, returnLoan } from "../../services/loans-api"
import type { Book } from "../../types/books"
import type { Loan, LoanInput } from "../../types/loans"
import { errorMessage, escapeHtml, formatDate, todayIso } from "../../utils/format"

type Filter = "ALL" | "ACTIVE" | "OVERDUE" | "RETURNED"

const FILTERS: { key: Filter; label: string }[] = [
    { key: "ALL", label: "Todos" },
    { key: "ACTIVE", label: "Ativos" },
    { key: "OVERDUE", label: "Atrasados" },
    { key: "RETURNED", label: "Devolvidos" },
]

const isOverdue = (loan: Loan) => loan.status === "ACTIVE" && loan.dueDate < todayIso()

const bookOptions = (books: Book[], loanedIds: Set<number>) =>
    books
        .map((book) => {
            const loaned = loanedIds.has(book.id)
            const label = `${book.title} - ${book.author}${loaned ? " · emprestado" : ""}`
            return `<option value="${book.id}" ${loaned ? "disabled" : ""}>${escapeHtml(label)}</option>`
        })
        .join("")

// alunos vem da copia local da library-api, entao a tela funciona mesmo com a students-api fora do ar
const studentOptions = (students: ReplicatedStudent[]) =>
    students
        .map((student) => {
            const active = student.status === "ATIVO"
            const label = `${student.name} (${student.enrollmentNumber ?? "-"})${active ? "" : ` · ${student.status}`}`
            return `<option value="${student.id}" ${active ? "" : "disabled"}>${escapeHtml(label)}</option>`
        })
        .join("")

const formHtml = (books: Book[], loanedIds: Set<number>, students: ReplicatedStudent[]) => `
    <label class="swal-form-label" for="loan-book">Livro</label>
    <select id="loan-book" class="swal2-select">${bookOptions(books, loanedIds)}</select>
    <label class="swal-form-label" for="loan-student">Estudante</label>
    <select id="loan-student" class="swal2-select">${studentOptions(students)}</select>
    <label class="swal-form-label" for="loan-due">Devolução prevista (padrão: 14 dias)</label>
    <input id="loan-due" class="swal2-input" type="date" min="${todayIso()}">
`

const readForm = (): LoanInput | null => {
    const value = (id: string) => (document.getElementById(id) as HTMLInputElement | null)?.value ?? ""
    const bookId = Number(value("loan-book"))
    const studentId = Number(value("loan-student"))

    if (!bookId || !studentId) {
        Swal.showValidationMessage("Selecione o livro e o estudante.")
        return null
    }

    return { bookId, studentId, loanDate: null, dueDate: value("loan-due") || null }
}

export default function LoanList() {
    const [loans, setLoans] = useState<Loan[]>([])
    const [filter, setFilter] = useState<Filter>("ALL")
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const fetchData = useCallback(async () => {
        try {
            setLoans(await getLoans())
            setError(null)
        } catch (err) {
            setError(errorMessage(err, "Não foi possível carregar os empréstimos."))
        } finally {
            setIsLoading(false)
        }
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

    const counts = useMemo(
        () => ({
            active: loans.filter((loan) => loan.status === "ACTIVE").length,
            overdue: loans.filter(isOverdue).length,
            returned: loans.filter((loan) => loan.status === "RETURNED").length,
        }),
        [loans],
    )

    const activeBookIds = useMemo(
        () => new Set(loans.filter((loan) => loan.status === "ACTIVE").map((loan) => loan.bookId)),
        [loans],
    )

    const visibleLoans = useMemo(() => {
        const sorted = [...loans].sort((a, b) => b.id - a.id)
        switch (filter) {
            case "ACTIVE":
                return sorted.filter((loan) => loan.status === "ACTIVE")
            case "OVERDUE":
                return sorted.filter(isOverdue)
            case "RETURNED":
                return sorted.filter((loan) => loan.status === "RETURNED")
            default:
                return sorted
        }
    }, [loans, filter])

    const handleNewLoan = async () => {
        let options: [Book[], ReplicatedStudent[]]
        try {
            options = await Promise.all([getBooks(), getReplicatedStudents()])
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível carregar livros e estudantes."), "error")
            return
        }
        const [books, students] = options

        if (books.length === 0 || students.length === 0) {
            await Swal.fire(
                "Nada para emprestar",
                books.length === 0
                    ? "Cadastre um livro antes de registrar empréstimos."
                    : "A cópia local de estudantes está vazia. Suba a students-api para os eventos chegarem.",
                "info",
            )
            return
        }

        const result = await Swal.fire({
            title: "Novo empréstimo",
            html: formHtml(books, activeBookIds, students),
            focusConfirm: false,
            showCancelButton: true,
            confirmButtonText: "Registrar",
            cancelButtonText: "Cancelar",
            preConfirm: readForm,
        })

        if (!result.isConfirmed || !result.value) {
            return
        }

        try {
            const loan = await createLoan(result.value)
            await load()
            await Swal.fire(
                "Empréstimo registrado",
                `"${loan.bookTitle}" para ${loan.studentName}, devolução até ${formatDate(loan.dueDate)}.`,
                "success",
            )
        } catch (err) {
            await Swal.fire("Não foi possível emprestar", errorMessage(err, "Erro ao registrar o empréstimo."), "error")
        }
    }

    const handleReturn = async (loan: Loan) => {
        const result = await Swal.fire({
            title: "Registrar devolução?",
            text: `"${loan.bookTitle}" emprestado para ${loan.studentName}.`,
            icon: "question",
            showCancelButton: true,
            confirmButtonText: "Devolver",
            cancelButtonText: "Cancelar",
        })

        if (!result.isConfirmed) {
            return
        }

        try {
            await returnLoan(loan.id)
            await load()
            await Swal.fire("Devolvido", "Devolução registrada.", "success")
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível registrar a devolução."), "error")
        }
    }

    const handleDelete = async (loan: Loan) => {
        const result = await Swal.fire({
            title: "Excluir empréstimo?",
            text: `O registro de "${loan.bookTitle}" para ${loan.studentName} será removido.`,
            icon: "warning",
            showCancelButton: true,
            confirmButtonText: "Excluir",
            cancelButtonText: "Cancelar",
        })

        if (!result.isConfirmed) {
            return
        }

        try {
            await deleteLoan(loan.id)
            await load()
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível excluir o empréstimo."), "error")
        }
    }

    const statusBadge = (loan: Loan) => {
        if (loan.status === "RETURNED") {
            return <Badge bg="secondary">Devolvido</Badge>
        }
        return isOverdue(loan) ? <Badge bg="danger">Atrasado</Badge> : <Badge bg="primary">Ativo</Badge>
    }

    return (
        <>
            <PageHeader
                eyebrow="library-api · publica em library.events"
                title="Empréstimos"
                description="Cada empréstimo valida o aluno na cópia local da library-api e avisa a students-api por evento."
                actions={
                    <Button variant="primary" onClick={handleNewLoan}>
                        <i className="bi bi-plus-lg me-2" />
                        Novo empréstimo
                    </Button>
                }
            />

            <div className="stat-grid">
                <StatCard label="Ativos" value={counts.active} icon="bi-bookmark-check" tone="primary" isLoading={isLoading} />
                <StatCard
                    label="Atrasados"
                    value={counts.overdue}
                    icon="bi-exclamation-triangle"
                    tone={counts.overdue > 0 ? "danger" : "neutral"}
                    isLoading={isLoading}
                />
                <StatCard label="Devolvidos" value={counts.returned} icon="bi-check2-all" tone="success" isLoading={isLoading} />
            </div>

            <section className="data-card">
                <div className="data-card-header">
                    <div className="filter-pills">
                        {FILTERS.map((item) => (
                            <button
                                key={item.key}
                                type="button"
                                className={`filter-pill${filter === item.key ? " active" : ""}`}
                                onClick={() => setFilter(item.key)}
                            >
                                {item.label}
                            </button>
                        ))}
                    </div>
                    {isLoading ? (
                        <Spinner animation="border" size="sm" />
                    ) : (
                        <Button variant="outline-primary" size="sm" onClick={load}>
                            <i className="bi bi-arrow-clockwise me-1" />
                            Atualizar
                        </Button>
                    )}
                </div>

                {error ? (
                    <div className="data-card-body">
                        <div className="alert alert-danger mb-0">{error}</div>
                    </div>
                ) : (
                    <Table responsive className="data-table">
                        <thead>
                            <tr>
                                <th>Livro</th>
                                <th>Estudante</th>
                                <th>Retirada</th>
                                <th>Devolução</th>
                                <th>Situação</th>
                                <th className="text-end">Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {visibleLoans.map((loan) => (
                                <tr key={loan.id}>
                                    <td>
                                        <span className="cell-main">{loan.bookTitle}</span>
                                        <span className="cell-sub">{loan.bookAuthor}</span>
                                    </td>
                                    <td>
                                        <span className="cell-main">{loan.studentName}</span>
                                        <span className="cell-sub">
                                            {loan.studentDataAvailable
                                                ? `${loan.studentEnrollmentNumber ?? "-"} · ${loan.studentCourseName ?? "sem curso"}`
                                                : "aluno fora da cópia local"}
                                        </span>
                                    </td>
                                    <td>{formatDate(loan.loanDate)}</td>
                                    <td>
                                        {loan.returnDate ? (
                                            <>
                                                <span className="cell-main">{formatDate(loan.returnDate)}</span>
                                                <span className="cell-sub">previsto {formatDate(loan.dueDate)}</span>
                                            </>
                                        ) : (
                                            <span className={isOverdue(loan) ? "overdue-text" : undefined}>
                                                {formatDate(loan.dueDate)}
                                            </span>
                                        )}
                                    </td>
                                    <td>{statusBadge(loan)}</td>
                                    <td className="text-end">
                                        <div className="row-actions">
                                            {loan.status === "ACTIVE" && (
                                                <Button variant="outline-primary" size="sm" onClick={() => handleReturn(loan)}>
                                                    <i className="bi bi-box-arrow-in-left me-1" />
                                                    Devolver
                                                </Button>
                                            )}
                                            <Button
                                                variant="outline-danger"
                                                size="sm"
                                                className="icon-btn"
                                                title="Excluir"
                                                onClick={() => handleDelete(loan)}
                                            >
                                                <i className="bi bi-trash" />
                                            </Button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {!isLoading && visibleLoans.length === 0 && (
                                <tr>
                                    <td colSpan={6}>
                                        <div className="empty-state">
                                            <i className="bi bi-inbox" />
                                            Nenhum empréstimo neste filtro.
                                        </div>
                                    </td>
                                </tr>
                            )}
                        </tbody>
                    </Table>
                )}
            </section>
        </>
    )
}
