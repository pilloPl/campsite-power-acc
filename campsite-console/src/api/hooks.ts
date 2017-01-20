import { useMutation, useQuery, useQueryClient, type UseQueryOptions } from '@tanstack/react-query';
import { post, errorMessage, type PostPath, type RequestOf, type ResponseOf } from './client';
import { useNotifier } from '../components/Notifier';

/** Reads a console operation; the query key is the path and the request, so equal requests share the cache. */
export function useApiQuery<P extends PostPath>(
  path: P,
  body: RequestOf<P>,
  options?: Omit<UseQueryOptions<ResponseOf<P>, Error>, 'queryKey' | 'queryFn'>,
) {
  return useQuery<ResponseOf<P>, Error>({
    queryKey: [path, body],
    queryFn: () => post(path, body),
    ...options,
  });
}

export interface ApiMutationOptions<P extends PostPath> {
  /** Paths of reads to refresh after the change (prefix match on the path). */
  invalidates?: string[];
  /** Message shown after success; a function receives the response. */
  success?: string | ((response: ResponseOf<P>) => string);
  onSuccess?: (response: ResponseOf<P>) => void;
}

/** Calls a state-changing console operation, shows its result or fault and refreshes the affected reads. */
export function useApiMutation<P extends PostPath>(path: P, options: ApiMutationOptions<P> = {}) {
  const queryClient = useQueryClient();
  const notify = useNotifier();
  return useMutation<ResponseOf<P>, Error, RequestOf<P>>({
    mutationFn: (body) => post(path, body),
    onSuccess: async (response) => {
      const prefixes = options.invalidates ?? [];
      if (prefixes.length > 0) {
        await queryClient.invalidateQueries({
          predicate: (query) => prefixes.some((prefix) => String(query.queryKey[0]).startsWith(prefix)),
        });
      }
      if (options.success) {
        notify(typeof options.success === 'function' ? options.success(response) : options.success, 'success');
      }
      options.onSuccess?.(response);
    },
    onError: (error) => notify(errorMessage(error), 'error'),
  });
}
