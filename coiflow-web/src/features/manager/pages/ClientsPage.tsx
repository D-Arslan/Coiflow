import { useState, type FormEvent } from 'react';
import { useClients, useCreateClient, useUpdateClient } from '@/features/manager/hooks/useClients';
import { useDebounce } from '@/shared/utils/useDebounce';
import { DataTable } from '@/shared/components/DataTable';
import { Modal } from '@/shared/components/Modal';
import type { Column } from '@/shared/components/DataTable';
import type { Client, CreateClientPayload, UpdateClientPayload } from '@/shared/types/client';

const columns: Column<Client>[] = [
  { header: 'Nom', accessor: (c) => `${c.lastName} ${c.firstName}` },
  { header: 'Telephone', accessor: 'phone' },
  { header: 'Email', accessor: 'email' },
  {
    header: 'Notes',
    accessor: (c) => c.notes && c.notes.length > 40 ? c.notes.substring(0, 40) + '...' : c.notes,
  },
];

export default function ClientsPage() {
  const [search, setSearch] = useState('');
  const debouncedSearch = useDebounce(search, 300);
  const { data: clients = [], isLoading } = useClients(debouncedSearch);
  const createMutation = useCreateClient();
  const updateMutation = useUpdateClient();

  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [editingClient, setEditingClient] = useState<Client | null>(null);

  const handleCreate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const payload: CreateClientPayload = {
      firstName: form.get('firstName') as string,
      lastName: form.get('lastName') as string,
      phone: (form.get('phone') as string) || undefined,
      email: (form.get('email') as string) || undefined,
      notes: (form.get('notes') as string) || undefined,
    };
    createMutation.mutate(payload, {
      onSuccess: () => setIsCreateOpen(false),
    });
  };

  const handleUpdate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!editingClient) return;
    const form = new FormData(e.currentTarget);
    const payload: UpdateClientPayload = {
      firstName: form.get('firstName') as string,
      lastName: form.get('lastName') as string,
      phone: (form.get('phone') as string) || undefined,
      email: (form.get('email') as string) || undefined,
      notes: (form.get('notes') as string) || undefined,
    };
    updateMutation.mutate(
      { id: editingClient.id, payload },
      { onSuccess: () => setEditingClient(null) },
    );
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-stone-800">Clients</h2>
        <button
          onClick={() => setIsCreateOpen(true)}
          className="rounded-md bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
        >
          Nouveau client
        </button>
      </div>

      <input
        type="text"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        placeholder="Rechercher par nom..."
        className="w-full max-w-sm rounded-md border border-stone-300 px-3 py-2 text-sm shadow-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
      />

      <DataTable<Client>
        columns={columns}
        data={clients}
        keyExtractor={(c) => c.id}
        isLoading={isLoading}
        emptyMessage="Aucun client"
        actions={(c) => (
          <button
            onClick={() => setEditingClient(c)}
            className="text-amber-600 hover:text-amber-700 text-sm font-medium"
          >
            Modifier
          </button>
        )}
      />

      {/* Create Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Nouveau client">
        <form onSubmit={handleCreate} className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <input name="firstName" required placeholder="Prenom *" className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
            <input name="lastName" required placeholder="Nom *" className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
          </div>
          <div className="grid grid-cols-2 gap-3">
            <input name="phone" placeholder="Telephone" className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
            <input name="email" type="email" placeholder="Email" className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
          </div>
          <textarea name="notes" rows={3} placeholder="Notes" className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setIsCreateOpen(false)} className="rounded-md border border-stone-300 px-4 py-2 text-sm text-stone-700 hover:bg-stone-50">
              Annuler
            </button>
            <button type="submit" disabled={createMutation.isPending} className="rounded-md bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700 disabled:opacity-50">
              {createMutation.isPending ? 'Creation...' : 'Creer'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Edit Modal */}
      <Modal isOpen={editingClient !== null} onClose={() => setEditingClient(null)} title="Modifier le client">
        {editingClient && (
          <form onSubmit={handleUpdate} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <input name="firstName" required defaultValue={editingClient.firstName} placeholder="Prenom *" className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
              <input name="lastName" required defaultValue={editingClient.lastName} placeholder="Nom *" className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <input name="phone" defaultValue={editingClient.phone ?? ''} placeholder="Telephone" className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
              <input name="email" type="email" defaultValue={editingClient.email ?? ''} placeholder="Email" className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
            </div>
            <textarea name="notes" rows={3} defaultValue={editingClient.notes ?? ''} placeholder="Notes" className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500" />
            <div className="flex justify-end gap-3 pt-2">
              <button type="button" onClick={() => setEditingClient(null)} className="rounded-md border border-stone-300 px-4 py-2 text-sm text-stone-700 hover:bg-stone-50">
                Annuler
              </button>
              <button type="submit" disabled={updateMutation.isPending} className="rounded-md bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700 disabled:opacity-50">
                {updateMutation.isPending ? 'Sauvegarde...' : 'Sauvegarder'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
