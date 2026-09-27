import { useCallback, useEffect, useMemo, useState } from "react"
import { Button, Spinner, Table } from "react-bootstrap"
import Swal from "sweetalert2"
import PageHeader from "../../components/PageHeader"
import StatCard from "../../components/StatCard"
import { createBook, deleteBook, getBooks, updateBook } from "../../services/books-api"
import { getLoans } from "../../services/loans-api"
import type { Book, BookInput } from "../../types/books"
import { errorMessage, escapeHtml } from "../../utils/format"

const formHtml = (book?: Book) => `
    <input id="book-title" class="swal2-input" placeholder="Título" value="${escapeHtml(book?.title ?? "")}">
    <input id="book-author" class="swal2-input" placeholder="Autor" value="${escapeHtml(book?.author ?? "")}">
`

const readForm = (): BookInput | null => {
    const title = (document.getElementById("book-title") as HTMLInputElement | null)?.value.trim()
    const author = (document.getElementById("book-author") as HTMLInputElement | null)?.value.trim()
    if (!title || !author) {
        Swal.showValidationMessage("Preencha título e autor.")
        return null
    }
    return { title, author }
}

export default function BookList() {
    const [books, setBooks] = useState<Book[]>([])
    const [loanedBookIds, setLoanedBookIds] = useState<Set<number>>(new Set())
    const [isLoading, setIsLoading] = useState(true)
    const [error, setError] = useState<string | null>(null)

    const fetchData = useCallback(async () => {
        try {
            const [bookData, loanData] = await Promise.all([getBooks(), getLoans()])
            setBooks(bookData)
            setLoanedBookIds(new Set(loanData.filter((loan) => loan.status === "ACTIVE").map((loan) => loan.bookId)))
            setError(null)
        } catch (err) {
            setError(errorMessage(err, "Não foi possível carregar os livros."))
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

    const uniqueAuthors = useMemo(() => new Set(books.map((book) => book.author)).size, [books])

    const handleAddBook = async () => {
        const result = await Swal.fire({
            title: "Novo livro",
            html: formHtml(),
            focusConfirm: false,
            showCancelButton: true,
            confirmButtonText: "Salvar",
            cancelButtonText: "Cancelar",
            preConfirm: readForm,
        })

        if (!result.isConfirmed || !result.value) {
            return
        }

        try {
            await createBook(result.value)
            await load()
            await Swal.fire("Salvo", "Livro cadastrado com sucesso.", "success")
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível salvar o livro."), "error")
        }
    }

    const handleEditBook = async (book: Book) => {
        const result = await Swal.fire({
            title: "Editar livro",
            html: formHtml(book),
            focusConfirm: false,
            showCancelButton: true,
            confirmButtonText: "Atualizar",
            cancelButtonText: "Cancelar",
            preConfirm: readForm,
        })

        if (!result.isConfirmed || !result.value) {
            return
        }

        try {
            await updateBook(book.id, result.value)
            await load()
            await Swal.fire("Atualizado", "Livro atualizado com sucesso.", "success")
        } catch (err) {
            await Swal.fire("Erro", errorMessage(err, "Não foi possível atualizar o livro."), "error")
        }
    }

    const handleDeleteBook = async (book: Book) => {
        const result = await Swal.fire({
            title: "Remover livro?",
            text: `Isso vai excluir "${book.title}".`,
            icon: "warning",
            showCancelButton: true,
            confirmButtonText: "Excluir",
            cancelButtonText: "Cancelar",
        })

        if (!result.isConfirmed) {
            return
        }

        try {
            await deleteBook(book.id)
            await load()
            await Swal.fire("Excluído", "Livro removido com sucesso.", "success")
        } catch (err) {
            // 409 quando o livro ainda esta emprestado
            await Swal.fire("Não foi possível excluir", errorMessage(err, "Erro ao remover o livro."), "error")
        }
    }

    return (
        <>
            <PageHeader
                eyebrow="library-api"
                title="Livros"
                description="Acervo da biblioteca. Livros com empréstimo ativo não podem ser removidos."
                actions={
                    <Button variant="primary" onClick={handleAddBook}>
                        <i className="bi bi-journal-plus me-2" />
                        Novo livro
                    </Button>
                }
            />

            <div className="stat-grid">
                <StatCard label="Títulos no acervo" value={books.length} icon="bi-journal-bookmark" isLoading={isLoading} />
                <StatCard label="Autores" value={uniqueAuthors} icon="bi-pen" tone="neutral" isLoading={isLoading} />
                <StatCard
                    label="Emprestados agora"
                    value={loanedBookIds.size}
                    icon="bi-bookmark-check"
                    tone="accent"
                    isLoading={isLoading}
                />
            </div>

            <section className="data-card">
                <div className="data-card-header">
                    <div>
                        <h2 className="data-card-title">Catálogo</h2>
                        <span className="data-card-subtitle">Servido pela library-api, rota /library-api do gateway</span>
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
                                <th>Título</th>
                                <th>Autor</th>
                                <th>Disponibilidade</th>
                                <th className="text-end">Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            {books.map((book) => (
                                <tr key={book.id}>
                                    <td>
                                        <span className="cell-main">{book.title}</span>
                                        <span className="cell-sub">#{book.id}</span>
                                    </td>
                                    <td>{book.author}</td>
                                    <td>
                                        {loanedBookIds.has(book.id) ? (
                                            <span className="source-chip">
                                                <i className="bi bi-bookmark-check" />
                                                emprestado
                                            </span>
                                        ) : (
                                            <span className="text-secondary small">disponível</span>
                                        )}
                                    </td>
                                    <td className="text-end">
                                        <div className="row-actions">
                                            <Button
                                                variant="outline-secondary"
                                                size="sm"
                                                className="icon-btn"
                                                title="Editar"
                                                onClick={() => handleEditBook(book)}
                                            >
                                                <i className="bi bi-pencil" />
                                            </Button>
                                            <Button
                                                variant="outline-danger"
                                                size="sm"
                                                className="icon-btn"
                                                title="Excluir"
                                                onClick={() => handleDeleteBook(book)}
                                            >
                                                <i className="bi bi-trash" />
                                            </Button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                            {!isLoading && books.length === 0 && (
                                <tr>
                                    <td colSpan={4}>
                                        <div className="empty-state">
                                            <i className="bi bi-journal" />
                                            Nenhum livro cadastrado ainda.
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
