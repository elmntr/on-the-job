'use client';

import { Check, Info } from 'lucide-react';
import {
  Toast, ToastClose, ToastContent, ToastPortal, ToastTitle,
  ToastViewport, useToastManager,
} from '@/components/ui/toast';

export function JournalToasts() {
  const { toasts } = useToastManager();
  return (
    <ToastPortal>
      <ToastViewport className="journal-toast-viewport">
        {toasts.map((item) => (
          <Toast key={item.id} toast={item} className="journal-toast">
            <ToastContent className="journal-toast-content">
              {item.type === 'success' ? <Check size={20} aria-hidden="true" /> : <Info size={20} aria-hidden="true" />}
              <ToastTitle className="journal-toast-title" />
              <ToastClose className="journal-toast-close" />
            </ToastContent>
          </Toast>
        ))}
      </ToastViewport>
    </ToastPortal>
  );
}
