import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { zodResolver } from '@hookform/resolvers/zod';
import { useCreateRssFeed } from '@/shared/api/hooks';
import { X } from 'lucide-react';

const rssSchema = z.object({
    name: z.string().min(1, '피드 이름을 입력해주세요'),
    url: z.string().url('올바른 URL 형식이 아닙니다'),
    category: z.enum(['DOMESTIC', 'DISCLOSURE', 'OVERSEAS', 'OTHER']),
});

type RssFormData = z.infer<typeof rssSchema>;

interface AddRssFeedFormProps {
    isOpen: boolean;
    onClose: () => void;
}

/**
 * RSS 피드 추가 폼.
 * React Hook Form + Zod 검증.
 */
export function AddRssFeedForm({ isOpen, onClose }: AddRssFeedFormProps) {
    const createFeed = useCreateRssFeed();

    const {
        register,
        handleSubmit,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<RssFormData>({
        resolver: zodResolver(rssSchema),
        defaultValues: {
            name: '',
            url: '',
            category: 'DOMESTIC',
        },
    });

    const onSubmit = async (data: RssFormData) => {
        try {
            await createFeed.mutateAsync(data);
            reset();
            onClose();
        } catch (error) {
            console.error('RSS 피드 추가 실패:', error);
        }
    };

    if (!isOpen) return null;

    const categories = [
        { value: 'DOMESTIC', label: '국내 뉴스' },
        { value: 'DISCLOSURE', label: '공시 (DART)' },
        { value: 'OVERSEAS', label: '해외 뉴스' },
        { value: 'OTHER', label: '기타' },
    ];

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
            <div className="w-full max-w-md rounded-xl border border-border bg-card p-6">
                {/* 헤더 */}
                <div className="mb-6 flex items-center justify-between">
                    <h2 className="text-lg font-semibold">RSS 피드 추가</h2>
                    <button
                        onClick={onClose}
                        className="rounded-lg p-1 hover:bg-secondary transition-colors"
                    >
                        <X className="h-5 w-5" />
                    </button>
                </div>

                {/* 폼 */}
                <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                    {/* 이름 */}
                    <div>
                        <label className="block text-sm font-medium mb-1.5">
                            피드 이름
                        </label>
                        <input
                            {...register('name')}
                            type="text"
                            placeholder="예: 네이버 경제"
                            className="w-full rounded-lg border border-border bg-secondary/30 px-4 py-2.5 focus:border-primary focus:outline-none"
                        />
                        {errors.name && (
                            <p className="mt-1 text-sm text-red-400">{errors.name.message}</p>
                        )}
                    </div>

                    {/* URL */}
                    <div>
                        <label className="block text-sm font-medium mb-1.5">
                            RSS URL
                        </label>
                        <input
                            {...register('url')}
                            type="url"
                            placeholder="https://example.com/rss.xml"
                            className="w-full rounded-lg border border-border bg-secondary/30 px-4 py-2.5 focus:border-primary focus:outline-none"
                        />
                        {errors.url && (
                            <p className="mt-1 text-sm text-red-400">{errors.url.message}</p>
                        )}
                    </div>

                    {/* 카테고리 */}
                    <div>
                        <label className="block text-sm font-medium mb-1.5">
                            카테고리
                        </label>
                        <select
                            {...register('category')}
                            className="w-full rounded-lg border border-border bg-secondary/30 px-4 py-2.5 focus:border-primary focus:outline-none"
                        >
                            {categories.map((cat) => (
                                <option key={cat.value} value={cat.value}>
                                    {cat.label}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* 버튼 */}
                    <div className="flex gap-3 pt-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="flex-1 rounded-lg border border-border py-2.5 hover:bg-secondary transition-colors"
                        >
                            취소
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="flex-1 rounded-lg bg-primary py-2.5 text-primary-foreground hover:bg-primary/90 transition-colors disabled:opacity-50"
                        >
                            {isSubmitting ? '추가 중...' : '추가'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
