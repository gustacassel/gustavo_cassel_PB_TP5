import { describe, expect, it } from "vitest"
import { errorMessage, escapeHtml, formatDate, toInputDate } from "./format"

describe("format", () => {
    it("formata LocalDate sem deslocar o dia por causa do fuso", () => {
        expect(formatDate("2026-09-27")).toBe("27/09/2026")
    })

    it("mostra traço quando não há data", () => {
        expect(formatDate(null)).toBe("-")
        expect(formatDate(undefined)).toBe("-")
    })

    it("devolve o valor original quando a data é inválida", () => {
        expect(formatDate("amanhã")).toBe("amanhã")
    })

    it("converte para o formato do input date", () => {
        expect(toInputDate("2026-10-11")).toBe("2026-10-11")
        expect(toInputDate(null)).toBe("")
    })

    it("escapa HTML antes de montar os formulários", () => {
        expect(escapeHtml('<b>"Clean" & Code</b>')).toBe("&lt;b&gt;&quot;Clean&quot; &amp; Code&lt;/b&gt;")
    })

    it("usa a mensagem do erro ou o texto padrão", () => {
        expect(errorMessage(new Error("aluno trancado"), "falhou")).toBe("aluno trancado")
        expect(errorMessage("qualquer coisa", "falhou")).toBe("falhou")
    })
})
