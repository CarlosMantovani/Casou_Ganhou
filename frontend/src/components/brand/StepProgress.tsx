interface StepProgressProps {
  currentStep: 1 | 2 | 3;
}

const steps = [
  { id: 1, label: 'Seus dados' },
  { id: 2, label: 'Quantidade' },
  { id: 3, label: 'Confirmação' },
] as const;

export function StepProgress({ currentStep }: StepProgressProps) {
  return (
    <ol aria-label="Progresso da compra" className="mx-auto flex max-w-72 items-center justify-center gap-3">
      {steps.map((step, index) => {
        const isDone = step.id < currentStep;
        const isCurrent = step.id === currentStep;

        return (
          <li className="flex items-center gap-3" key={step.id}>
            <div className="flex flex-col items-center gap-1">
              <span
                className={`grid h-8 w-8 place-items-center rounded-full text-xs font-bold ${
                  isDone || isCurrent ? 'bg-gold text-charcoal' : 'bg-ivory-deep text-warm-gray'
                }`}
              >
                {isDone ? '✓' : step.id}
              </span>
              <span className={`text-[11px] ${isCurrent ? 'font-semibold text-green' : 'text-warm-gray'}`}>
                {step.label}
              </span>
            </div>
            {index < steps.length - 1 ? <span aria-hidden="true" className="h-px w-8 bg-line" /> : null}
          </li>
        );
      })}
    </ol>
  );
}
