import { z } from 'zod';

const optionalEmailSchema = z.preprocess(
  (value) => (typeof value === 'string' && value.trim() === '' ? undefined : value),
  z.string().email('Informe um e-mail valido.').optional(),
);

export const adminLoginSchema = z.object({
  username: z.string().min(1, 'Informe o usuario.'),
  password: z.string().min(1, 'Informe a senha.'),
});

export const cashPaymentSchema = z.object({
  name: z.string().trim().min(1, 'Informe o nome.'),
  phone: z
    .string()
    .trim()
    .min(1, 'Informe o telefone.')
    .refine((value) => {
      const digits = value.replace(/\D/g, '');
      return digits.length === 10 || digits.length === 11;
    }, 'Informe um telefone com DDD.'),
  email: optionalEmailSchema,
  quantity: z.coerce.number().int().min(1, 'Informe ao menos 1 numero.'),
});

export const raffleConfigSchema = z.object({
  unitPrice: z.coerce
    .number({ invalid_type_error: 'Informe um valor valido.' })
    .positive('Informe um valor maior que zero.')
    .refine((value) => Number.isFinite(value), 'Informe um valor valido.')
    .refine((value) => Math.round(value * 100) === value * 100, 'Informe no maximo 2 casas decimais.'),
});

export const scheduledDrawSchema = z.object({
  scheduledDrawAt: z.string().min(1, 'Informe a data e horario do sorteio.'),
});

export type AdminLoginFormData = z.infer<typeof adminLoginSchema>;
export type CashPaymentFormData = z.infer<typeof cashPaymentSchema>;
export type RaffleConfigFormData = z.infer<typeof raffleConfigSchema>;
export type ScheduledDrawFormData = z.infer<typeof scheduledDrawSchema>;
