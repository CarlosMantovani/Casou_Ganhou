import { z } from 'zod';

const optionalEmailSchema = z.preprocess(
  (value) => (typeof value === 'string' && value.trim() === '' ? undefined : value),
  z.string().email('Informe um e-mail valido.').optional(),
);

export const buyerSchema = z.object({
  name: z.string().trim().min(1, 'Informe seu nome.'),
  phone: z
    .string()
    .trim()
    .min(1, 'Informe seu telefone.')
    .refine((value) => {
      const digits = value.replace(/\D/g, '');
      return digits.length === 10 || digits.length === 11;
    }, 'Informe um telefone com DDD.'),
  email: optionalEmailSchema,
});

export type BuyerFormData = z.infer<typeof buyerSchema>;
