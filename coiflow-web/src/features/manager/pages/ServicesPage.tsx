import { useState, type FormEvent } from 'react';
import { useServices, useCreateService, useUpdateService, useDeleteService } from '@/features/manager/hooks/useServices';
import { DataTable } from '@/shared/components/DataTable';
import { Modal } from '@/shared/components/Modal';
import { formatPrice, formatDuration } from '@/shared/utils/formatters';
import type { Column } from '@/shared/components/DataTable';
import type { ServiceItem, CreateServicePayload, UpdateServicePayload } from '@/shared/types/service';

const columns: Column<ServiceItem>[] = [
  { header: 'Nom', accessor: 'name' },
  { header: 'Duree', accessor: (s) => formatDuration(s.durationMinutes) },
  { header: 'Prix', accessor: (s) => formatPrice(s.price) },
];

export default function ServicesPage() {
  const { data: services = [], isLoading } = useServices();
  const createMutation = useCreateService();
  const updateMutation = useUpdateService();
  const deleteMutation = useDeleteService();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingService, setEditingService] = useState<ServiceItem | null>(null);

  const handleCreate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const payload: CreateServicePayload = {
      name: form.get('name') as string,
      durationMinutes: Number(form.get('durationMinutes')),
      price: Number(form.get('price')),
    };
    createMutation.mutate(payload, {
      onSuccess: () => setIsCreateOpen(false),
    });
  };

  const handleUpdate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingService) return;
    const form = new FormData(e.currentTarget);
    const payload: UpdateServicePayload = {
      name: form.get('name') as string,
      durationMinutes: Number(form.get('durationMinutes')),
      price: Number(form.get('price')),
    };
    updateMutation.mutate(
      { id: editingService.id, payload },
      { onSuccess: () => setEditingService(null) },
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900">Prestations</h2>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700"
        >
          Nouvelle prestation
        </button>
      </div>

      <DataTable<ServiceItem>
        columns={columns}
        data={services}
        keyExtractor={(s) => s.id}
        isLoading={isLoading}
        emptyMessage="Aucune prestation"
        actions={(s) => (
          <div className="flex gap-2 justify-end">
            <button
              onClick={() => setEditingService(s)}
              className="text-blue-600 hover:text-blue-800 text-sm font-medium"
            >
              Modifier
            </button>
            <button
              onClick={() => { if (confirm('Desactiver cette prestation ?')) deleteMutation.mutate(s.id); }}
              className="text-red-600 hover:text-red-800 text-sm font-medium"
            >
              Desactiver
            </button>
          </div>
        )}
      />

      {/* Create Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Nouvelle prestation">
        <form onSubmit={handleCreate} className="space-y-4">
          <input name="name" required placeholder="Nom de la prestation *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          <div className="grid grid-cols-2 gap-3">
            <input name="durationMinutes" type="number" required min="5" max="480" placeholder="Duree (min) *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <input name="price" type="number" required min="0.01" step="0.01" placeholder="Prix (EUR) *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setIsCreateOpen(false)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
              Annuler
            </button>
            <button type="submit" disabled={createMutation.isPending} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
              {createMutation.isPending ? 'Creation...' : 'Creer'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Modal */}
      <Modal isOpen={editingService !== null} onClose={() => setEditingService(null)} title="Modifier la prestation">
        {editingService && (
          <form onSubmit={handleUpdate} className="space-y-4">
            <input name="name" required defaultValue={editingService.name} placeholder="Nom *" className="w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            <div className="grid grid-cols-2 gap-3">
              <input name="durationMinutes" type="number" required min="5" max="480" defaultValue={editingService.durationMinutes} placeholder="Duree (min) *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
              <input name="price" type="number" required min="0.01" step="0.01" defaultValue={Number(editingService.price)} placeholder="Prix (EUR) *" className="rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500" />
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setEditingService(null)} className="rounded-md border border-gray-300 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                Annuler
              </button>
              <button type="submit" disabled={updateMutation.isPending} className="rounded-md bg-blue-600 px-4 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50">
                {updateMutation.isPending ? 'Sauvegarde...' : 'Sauvegarder'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
