// Smoke test do sistema rodando (docker compose ou Kubernetes), tudo pelo api-gateway.
// Uso: node scripts/smoke-test.mjs [http://localhost:8000]

const GATEWAY = process.argv[2] ?? process.env.GATEWAY_URL ?? "http://localhost:8000"
const ZIPKIN = process.env.ZIPKIN_URL ?? "http://localhost:9411"
const CHECK_TRACING = process.env.CHECK_TRACING !== "false"

let failures = 0

const step = async (name, fn) => {
    try {
        const detail = await fn()
        console.log(`  ok   ${name}${detail ? ` (${detail})` : ""}`)
    } catch (err) {
        failures++
        console.log(`  FAIL ${name}: ${err.message}`)
    }
}

const call = async (path, options = {}) => {
    const response = await fetch(GATEWAY + path, {
        ...options,
        headers: { "Content-Type": "application/json" },
        body: options.body ? JSON.stringify(options.body) : undefined,
        signal: AbortSignal.timeout(10_000),
    })
    const text = await response.text()
    return { status: response.status, body: text ? JSON.parse(text) : null }
}

const expectStatus = (response, expected) => {
    if (response.status !== expected) {
        throw new Error(`esperado ${expected}, recebido ${response.status}: ${JSON.stringify(response.body)}`)
    }
    return response.body
}

// espera ate a condicao ser verdadeira (eventos sao assincronos)
const eventually = async (description, check, timeoutMs = 30_000) => {
    const start = Date.now()
    let last
    while (Date.now() - start < timeoutMs) {
        try {
            last = await check()
            if (last) return last
        } catch (err) {
            last = err.message
        }
        await new Promise((resolve) => setTimeout(resolve, 1000))
    }
    throw new Error(`${description} nao aconteceu em ${timeoutMs / 1000}s (ultimo: ${JSON.stringify(last)})`)
}

console.log(`Smoke test em ${GATEWAY}`)
const suffix = Date.now().toString().slice(-6)

await step("gateway, library-api e students-api saudaveis", async () => {
    await eventually("health UP", async () => {
        const services = await Promise.all([
            call("/actuator/health"),
            call("/library-api/actuator/health"),
            call("/students-api/actuator/health"),
        ])
        return services.every((s) => s.status === 200 && s.body.status === "UP")
    }, 120_000)
    return "UP"
})

await step("front-end servido pelo gateway", async () => {
    const response = await fetch(GATEWAY + "/loans")
    const html = await response.text()
    if (response.status !== 200 || !html.includes("Biblioteca Aurora")) throw new Error(`status ${response.status}`)
    return "SPA ok"
})

let studentId
await step("aluno cadastrado na students-api chega na copia local da library-api", async () => {
    const student = expectStatus(await call("/students-api/api/students", {
        method: "POST",
        body: { name: `Smoke ${suffix}`, email: `smoke${suffix}@infnet.edu.br`, enrollmentNumber: `SMK-${suffix}`, courseId: 1 },
    }), 201)
    studentId = student.id
    await eventually("student.created na library-api", async () =>
        (await call(`/library-api/api/integration/students/${studentId}`)).status === 200)
    return `aluno ${studentId}`
})

let loanId
await step("emprestimo validado pela copia local", async () => {
    const book = expectStatus(await call("/library-api/api/books", {
        method: "POST",
        body: { title: `Livro Smoke ${suffix}`, author: "Smoke Test" },
    }), 200)
    const loan = expectStatus(await call("/library-api/api/loans", {
        method: "POST",
        body: { bookId: book.id, studentId },
    }), 201)
    loanId = loan.id
    return `emprestimo ${loanId}`
})

await step("loan.created atualiza os emprestimos ativos na students-api", async () => {
    await eventually("activeLoans = 1", async () =>
        (await call(`/students-api/api/students/${studentId}`)).body?.activeLoans === 1)
    return "activeLoans = 1"
})

await step("aluno com emprestimo ativo nao pode ser excluido", async () => {
    expectStatus(await call(`/students-api/api/students/${studentId}`, { method: "DELETE" }), 409)
    return "409"
})

await step("devolucao libera a exclusao do aluno", async () => {
    expectStatus(await call(`/library-api/api/loans/${loanId}/return`, { method: "PUT" }), 200)
    await eventually("activeLoans = 0", async () =>
        (await call(`/students-api/api/students/${studentId}`)).body?.activeLoans === 0)
    expectStatus(await call(`/students-api/api/students/${studentId}`, { method: "DELETE" }), 204)
    await eventually("student.deleted na library-api", async () =>
        (await call(`/library-api/api/integration/students/${studentId}`)).status === 404)
    return "204"
})

if (CHECK_TRACING) {
    await step("trace distribuido registrado no Zipkin", async () => {
        const services = await eventually("servicos no Zipkin", async () => {
            const response = await fetch(`${ZIPKIN}/api/v2/services`)
            const list = await response.json()
            return ["api-gateway", "library-api", "students-api"].every((s) => list.includes(s)) && list
        })
        return services.join(", ")
    })
}

console.log(failures === 0 ? "\nTodos os passos passaram." : `\n${failures} passo(s) falharam.`)
process.exit(failures === 0 ? 0 : 1)
