import { useState } from 'react';
import { AlertTriangle, X } from 'lucide-react';

interface KillSwitchModalProps {
    isOpen: boolean;
    onClose: () => void;
    stockCode?: string; // 특정 종목만 매도할 경우
    stockName?: string;
}

/**
 * Kill Switch 긴급 매도 모달.
 * 전체 또는 특정 종목 일괄 매도 확인.
 */
export function KillSwitchModal({ isOpen, onClose, stockCode, stockName }: KillSwitchModalProps) {
    const [reason, setReason] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [confirmed, setConfirmed] = useState(false);

    const isTargetAll = !stockCode;
    const targetText = isTargetAll ? '전체 보유 종목' : `${stockName} (${stockCode})`;

    const handleActivate = async () => {
        if (!confirmed) return;

        setIsLoading(true);
        try {
            const endpoint = isTargetAll
                ? '/api/system/kill-switch'
                : `/api/system/kill-switch/${stockCode}`;

            await fetch(`http://localhost:8080${endpoint}`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ reason: reason || '사용자 요청' }),
            });

            alert('긴급 매도 명령이 실행되었습니다.');
            onClose();
        } catch (error) {
            console.error('Kill Switch 실행 실패:', error);
            alert('긴급 매도 실행에 실패했습니다.');
        } finally {
            setIsLoading(false);
            setConfirmed(false);
            setReason('');
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
            <div className="w-full max-w-md rounded-xl border border-red-500/50 bg-card p-6 shadow-2xl">
                {/* 헤더 */}
                <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-full bg-red-500/20">
                            <AlertTriangle className="h-6 w-6 text-red-500" />
                        </div>
                        <h2 className="text-xl font-bold text-red-400">Kill Switch</h2>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 text-muted-foreground hover:text-foreground transition-colors"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* 경고 메시지 */}
                <div className="rounded-lg bg-red-500/10 border border-red-500/30 p-4 mb-4">
                    <p className="text-sm text-red-300">
                        ⚠️ <strong>{targetText}</strong>을 즉시 시장가로 매도합니다.
                        이 작업은 되돌릴 수 없습니다.
                    </p>
                </div>

                {/* 사유 입력 */}
                <div className="mb-4">
                    <label className="block text-sm text-muted-foreground mb-2">
                        매도 사유 (선택)
                    </label>
                    <input
                        type="text"
                        value={reason}
                        onChange={(e) => setReason(e.target.value)}
                        placeholder="예: 시장 급락, 손절선 도달"
                        className="w-full rounded-lg border border-border bg-secondary/30 px-4 py-2 text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
                    />
                </div>

                {/* 확인 체크박스 */}
                <label className="flex items-center gap-3 mb-6 cursor-pointer">
                    <input
                        type="checkbox"
                        checked={confirmed}
                        onChange={(e) => setConfirmed(e.target.checked)}
                        className="h-5 w-5 rounded border-border bg-secondary/30 text-red-500 focus:ring-red-500"
                    />
                    <span className="text-sm text-muted-foreground">
                        위 내용을 확인했으며, 긴급 매도를 실행합니다.
                    </span>
                </label>

                {/* 버튼 */}
                <div className="flex gap-3">
                    <button
                        onClick={onClose}
                        className="flex-1 rounded-lg border border-border py-3 text-muted-foreground hover:bg-secondary/50 transition-colors"
                    >
                        취소
                    </button>
                    <button
                        onClick={handleActivate}
                        disabled={!confirmed || isLoading}
                        className="flex-1 rounded-lg bg-red-500 py-3 font-semibold text-white hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        {isLoading ? '실행 중...' : '🚨 긴급 매도 실행'}
                    </button>
                </div>
            </div>
        </div>
    );
}
