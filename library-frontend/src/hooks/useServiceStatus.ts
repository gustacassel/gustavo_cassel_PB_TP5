import { useEffect, useState } from "react"
import { LIBRARY_API_URL, STUDENTS_API_URL } from "../services/api-client"

export type ServiceState = "checking" | "online" | "offline"

export interface ServiceStatus {
    library: ServiceState
    students: ServiceState
}

const REFRESH_MS = 10_000

const ping = async (url: string): Promise<ServiceState> => {
    try {
        const response = await fetch(url, { signal: AbortSignal.timeout(3000) })
        return response.ok ? "online" : "offline"
    } catch {
        return "offline"
    }
}

export function useServiceStatus(): ServiceStatus {
    const [status, setStatus] = useState<ServiceStatus>({ library: "checking", students: "checking" })

    useEffect(() => {
        let isMounted = true

        const check = async () => {
            const [library, students] = await Promise.all([
                ping(`${LIBRARY_API_URL}/api/integration/students/health`),
                ping(`${STUDENTS_API_URL}/api/courses`),
            ])
            if (isMounted) {
                setStatus({ library, students })
            }
        }

        check()
        const timer = setInterval(check, REFRESH_MS)

        return () => {
            isMounted = false
            clearInterval(timer)
        }
    }, [])

    return status
}
