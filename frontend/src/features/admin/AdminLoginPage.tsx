import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation } from '@tanstack/react-query';
import { LockKeyhole } from 'lucide-react';
import { useForm } from 'react-hook-form';

import { Button } from '../../components/ui/Button';
import { Card } from '../../components/ui/Card';
import { TextInput } from '../../components/ui/TextInput';
import { authService } from '../../services/authService';
import { useAuth } from './AuthContext';
import { adminLoginSchema, type AdminLoginFormData } from './schemas';

export function AdminLoginPage() {
  const { refreshSession } = useAuth();
  const {
    formState: { errors, isValid },
    handleSubmit,
    register,
  } = useForm<AdminLoginFormData>({
    defaultValues: { username: '', password: '' },
    mode: 'onChange',
    resolver: zodResolver(adminLoginSchema),
  });

  const loginMutation = useMutation({
    mutationFn: (request: AdminLoginFormData) => authService.login(request),
    onSuccess: () => {
      refreshSession();
      window.history.replaceState({}, '', '/admin');
      window.dispatchEvent(new PopStateEvent('popstate'));
    },
  });

  return (
    <main className="grid min-h-screen place-items-center bg-cream px-6 py-12 text-charcoal">
      <div className="w-full max-w-sm text-center">
        <div className="mx-auto mb-6 grid h-14 w-14 place-items-center rounded-lg bg-charcoal text-white">
          <LockKeyhole aria-hidden="true" className="h-6 w-6" />
        </div>
        <h1 className="font-serif text-3xl font-bold">Área Administrativa</h1>
        <p className="mt-2 text-sm text-warm-gray">Presente Premiado</p>

        <Card className="mt-8 text-left">
          <form className="space-y-5" onSubmit={handleSubmit((data) => loginMutation.mutate(data))}>
            <TextInput
              autoComplete="username"
              error={errors.username?.message}
              id="admin-username"
              label="Usuário"
              placeholder="admin"
              {...register('username')}
            />
            <TextInput
              autoComplete="current-password"
              error={errors.password?.message}
              id="admin-password"
              label="Senha"
              placeholder="Sua senha"
              type="password"
              {...register('password')}
            />
            {loginMutation.isError ? (
              <p className="rounded-lg border border-terracotta/30 bg-blush px-4 py-3 text-sm text-terracotta-dark" role="alert">
                Usuário ou senha incorretos.
              </p>
            ) : null}
            <Button disabled={!isValid} isLoading={loginMutation.isPending} type="submit">
              Entrar
            </Button>
          </form>
        </Card>

        <p className="mt-6 text-xs text-warm-gray">Acesso restrito somente a administradores autorizados.</p>
      </div>
    </main>
  );
}
