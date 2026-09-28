import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"
import PageHeader from "./PageHeader"
import StatCard from "./StatCard"

describe("StatCard", () => {
    it("mostra o rótulo, o valor e a dica", () => {
        render(<StatCard label="Empréstimos ativos" value={3} icon="bi-bookmark" hint="1 em atraso" />)

        expect(screen.getByText("Empréstimos ativos")).toBeInTheDocument()
        expect(screen.getByText("3")).toBeInTheDocument()
        expect(screen.getByText("1 em atraso")).toBeInTheDocument()
    })

    it("mostra o carregamento no lugar do valor", () => {
        render(<StatCard label="Livros" value={10} icon="bi-book" isLoading />)

        expect(screen.queryByText("10")).not.toBeInTheDocument()
        expect(screen.getByRole("status")).toBeInTheDocument()
    })
})

describe("PageHeader", () => {
    it("mostra o título, a origem e as ações", () => {
        render(<PageHeader eyebrow="library-api" title="Empréstimos" actions={<button>Novo empréstimo</button>} />)

        expect(screen.getByRole("heading", { name: "Empréstimos" })).toBeInTheDocument()
        expect(screen.getByText("library-api")).toBeInTheDocument()
        expect(screen.getByRole("button", { name: "Novo empréstimo" })).toBeInTheDocument()
    })
})
