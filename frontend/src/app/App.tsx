import { BrowserRouter } from 'react-router-dom'
import { AppProvider } from './providers/AppProvider'
import { AppRoutes } from './routes/AppRoutes'
import './styles/globals.css'

/**
 * 애플리케이션 진입점.
 * frontendrule.md §1.1 준수 - Provider/Routes 분리
 */
export function App() {
    return (
        <AppProvider>
            <BrowserRouter>
                <AppRoutes />
            </BrowserRouter>
        </AppProvider>
    )
}
