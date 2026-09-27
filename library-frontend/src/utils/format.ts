export const escapeHtml = (value: string) =>
    value.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;")

// datas LocalDate (yyyy-MM-dd) sao lidas como data local, sem fuso
const parseDate = (value: string) => {
    const onlyDate = /^\d{4}-\d{2}-\d{2}$/.test(value)
    return onlyDate ? new Date(`${value}T00:00:00`) : new Date(value)
}

export const formatDate = (value: string | null | undefined) => {
    if (!value) {
        return "-"
    }

    const date = parseDate(value)
    if (Number.isNaN(date.getTime())) {
        return value
    }

    return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short" }).format(date)
}

export const formatDateTime = (value: string | null | undefined) => {
    if (!value) {
        return "-"
    }

    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
        return value
    }

    return new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "medium" }).format(date)
}

export const toInputDate = (value: string | null | undefined) => {
    if (!value) {
        return ""
    }

    const date = parseDate(value)
    if (Number.isNaN(date.getTime())) {
        return value
    }

    return date.toISOString().split("T")[0]
}

export const todayIso = () => new Date().toLocaleDateString("sv-SE")

export const errorMessage = (err: unknown, fallback: string) => (err instanceof Error ? err.message : fallback)
