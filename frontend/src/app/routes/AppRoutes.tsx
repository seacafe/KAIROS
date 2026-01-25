import { Routes, Route } from 'react-router-dom'
import { lazy, Suspense } from 'react'
import { MainLayout } from '@/widgets/layout/MainLayout'

// Lazy Loading (frontendrule.md §4 Code Splitting)
const DashboardPage = lazy(() => import('@/pages/dashboard/DashboardPage').then(m => ({ default: m.DashboardPage })))
const JournalPage = lazy(() => import('@/pages/journal/JournalPage').then(m => ({ default: m.JournalPage })))
const JournalDetailPage = lazy(() => import('@/pages/journal/JournalDetailPage').then(m => ({ default: m.JournalDetailPage })))
const DeepAnalysisPage = lazy(() => import('@/pages/analysis/DeepAnalysisPage').then(m => ({ default: m.DeepAnalysisPage })))
const SettingsPage = lazy(() => import('@/pages/settings/SettingsPage').then(m => ({ default: m.SettingsPage })))

/**
 * 애플리케이션 라우트 설정.
 * frontendrule.md §1.1 준수 - app/routes/ 분리
 */
export function AppRoutes() {
    return (
        <Routes>
            <Route path="/" element={<MainLayout />}>
                <Route
                    index
                    element={
                        <Suspense fallback={<PageLoader />}>
                            <DashboardPage />
                        </Suspense>
                    }
                />
                <Route
                    path="journal"
                    element={
                        <Suspense fallback={<PageLoader />}>
                            <JournalPage />
                        </Suspense>
                    }
                />
                <Route
                    path="journal/:date"
                    element={
                        <Suspense fallback={<PageLoader />}>
                            <JournalDetailPage />
                        </Suspense>
                    }
                />
                <Route
                    path="analysis"
                    element={
                        <Suspense fallback={<PageLoader />}>
                            <DeepAnalysisPage />
                        </Suspense>
                    }
                />
                <Route
                    path="settings"
                    element={
                        <Suspense fallback={<PageLoader />}>
                            <SettingsPage />
                        </Suspense>
                    }
                />
            </Route>
        </Routes>
    )
}

function PageLoader() {
    return (
        <div className="flex h-full items-center justify-center">
            <div className="text-muted-foreground">Loading...</div>
        </div>
    )
}

