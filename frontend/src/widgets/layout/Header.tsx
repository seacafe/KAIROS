import { Bell, User } from 'lucide-react'
import { useAccountStore } from '@/stores/accountStore'

/**
 * 헤더 - 자산 현황 및 사용자 정보
 */
export function Header() {
    const { totalAsset, dailyProfit } = useAccountStore()

    const isProfitable = dailyProfit >= 0
    const profitClass = isProfitable ? 'text-profit' : 'text-loss'
    const profitSign = isProfitable ? '+' : ''

    return (
        <header className="flex h-16 items-center justify-between border-b border-border bg-card px-6">
            {/* 자산 현황 */}
            <div className="flex items-center gap-8">
                <div>
                    <div className="text-xs text-muted-foreground">총 자산</div>
                    <div className="font-mono text-lg font-semibold">
                        ₩{totalAsset.toLocaleString()}
                    </div>
                </div>
                <div>
                    <div className="text-xs text-muted-foreground">당일 손익</div>
                    <div className={`font-mono text-lg font-semibold ${profitClass}`}>
                        {profitSign}₩{Math.abs(dailyProfit).toLocaleString()}
                    </div>
                </div>
            </div>

            {/* 우측 아이콘 */}
            <div className="flex items-center gap-4">
                <button className="rounded-lg p-2 text-muted-foreground hover:bg-accent hover:text-accent-foreground">
                    <Bell className="h-5 w-5" />
                </button>
                <button className="rounded-lg p-2 text-muted-foreground hover:bg-accent hover:text-accent-foreground">
                    <User className="h-5 w-5" />
                </button>
            </div>
        </header>
    )
}
