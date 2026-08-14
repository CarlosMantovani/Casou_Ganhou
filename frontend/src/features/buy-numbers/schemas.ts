import { z } from 'zod';

export const buyerEmailSchema = z.object({
  email: z.string().min(1, 'Informe seu e-mail.').email('Informe um e-mail válido.'),
});

export type BuyerEmailFormData = z.infer<typeof buyerEmailSchema>;
