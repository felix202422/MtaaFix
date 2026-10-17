import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { api, isApiError } from '../../lib/api';
import type { Category } from '../../lib/types';
import {
  Badge,
  Button,
  Card,
  EmptyState,
  ErrorState,
  Field,
  LoadingState,
  useToast,
} from '../../design-system';
import { AdminPageShell } from './AdminPageShell';

export function AdminCategoriesPage() {
  const { push } = useToast();
  const [categories, setCategories] = useState<Category[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  const [newName, setNewName] = useState('');
  const [newSlug, setNewSlug] = useState('');
  const [newType, setNewType] = useState('PRIMARY');

  const load = useCallback(() => {
    setCategories(null);
    setError(null);
    api
      .listAdminCategories()
      .then(setCategories)
      .catch((err) => setError(isApiError(err) ? err.message : 'Could not load categories.'));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function mutate(category: Category, action: () => Promise<unknown>, message: string) {
    setBusyId(category.id);
    try {
      await action();
      push(message, 'success');
      load();
    } catch (err) {
      push(isApiError(err) ? err.message : 'The action failed.', 'error');
    } finally {
      setBusyId(null);
    }
  }

  async function create(event: FormEvent) {
    event.preventDefault();
    if (!newName.trim() || !newSlug.trim()) return;
    try {
      await api.createCategory({
        name: newName.trim(),
        slug: newSlug.trim(),
        type: newType,
      });
      push('Category created.', 'success');
      setNewName('');
      setNewSlug('');
      load();
    } catch (err) {
      push(isApiError(err) ? err.message : 'Could not create the category.', 'error');
    }
  }

  return (
    <AdminPageShell
      title="Category management"
      description="Create, edit, activate, deactivate and order report categories — validations and audit events apply (FR-5.1, P20)."
    >
      <Card aria-label="Create category">
        <h2 className="mb-3 text-base font-semibold text-brand-black">New category</h2>
        <form className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" onSubmit={create}>
          <Field
            label="Name"
            value={newName}
            onChange={(event) => setNewName(event.target.value)}
          />
          <Field
            label="Slug"
            hint="lowercase letters, digits, single dashes"
            value={newSlug}
            onChange={(event) => setNewSlug(event.target.value)}
          />
          <div>
            <label htmlFor="cat-type" className="mb-1 block text-sm font-medium">
              Type
            </label>
            <select
              id="cat-type"
              className="w-full rounded-lg border border-line bg-surface px-3 py-2 text-base"
              value={newType}
              onChange={(event) => setNewType(event.target.value)}
            >
              <option value="PRIMARY">Primary</option>
              <option value="SECONDARY">Secondary</option>
            </select>
          </div>
          <div className="flex items-end">
            <Button type="submit" disabled={!newName.trim() || !newSlug.trim()}>
              Create category
            </Button>
          </div>
        </form>
      </Card>

      {error && <ErrorState message={error} onRetry={load} />}
      {!error && !categories && <LoadingState label="Loading categories…" />}
      {categories && categories.length === 0 && <EmptyState title="No categories yet." />}
      {categories && categories.length > 0 && (
        <Card padded={false} aria-label="Categories table">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-line bg-canvas text-xs uppercase text-muted">
                  <th className="px-4 py-3">Name</th>
                  <th className="px-4 py-3">Slug</th>
                  <th className="px-4 py-3">Type</th>
                  <th className="px-4 py-3">Order</th>
                  <th className="px-4 py-3">Status</th>
                  <th className="px-4 py-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {categories.map((category) => (
                  <tr key={category.id} className="border-b border-line/60">
                    <td className="px-4 py-3">
                      <label className="sr-only" htmlFor={`name-${category.id}`}>
                        Name for {category.slug}
                      </label>
                      <input
                        id={`name-${category.id}`}
                        className="rounded-lg border border-line bg-surface px-2 py-1 text-sm"
                        defaultValue={category.name}
                        onBlur={(event) => {
                          const next = event.target.value.trim();
                          if (next && next !== category.name) {
                            void mutate(
                              category,
                              () => api.updateCategory(category.id, { name: next }),
                              'Category updated.',
                            );
                          }
                        }}
                      />
                    </td>
                    <td className="px-4 py-3 text-muted">{category.slug}</td>
                    <td className="px-4 py-3 text-muted">{category.type}</td>
                    <td className="px-4 py-3">
                      <label className="sr-only" htmlFor={`order-${category.id}`}>
                        Display order for {category.slug}
                      </label>
                      <input
                        id={`order-${category.id}`}
                        type="number"
                        className="w-20 rounded-lg border border-line bg-surface px-2 py-1 text-sm"
                        defaultValue={category.displayOrder ?? ''}
                        onBlur={(event) => {
                          const raw = event.target.value.trim();
                          if (raw === '') return;
                          const next = Number(raw);
                          if (Number.isFinite(next) && next !== category.displayOrder) {
                            void mutate(
                              category,
                              () => api.setCategoryOrder(category.id, next),
                              'Display order updated.',
                            );
                          }
                        }}
                      />
                    </td>
                    <td className="px-4 py-3">
                      <Badge tone={category.active === false ? 'error' : 'success'}>
                        {category.active === false ? 'Inactive' : 'Active'}
                      </Badge>
                    </td>
                    <td className="px-4 py-3">
                      <Button
                        size="sm"
                        variant={category.active === false ? 'secondary' : 'outline'}
                        disabled={busyId === category.id}
                        onClick={() =>
                          void mutate(
                            category,
                            () => api.setCategoryActive(category.id, category.active === false),
                            category.active === false
                              ? 'Category activated.'
                              : 'Category deactivated — hidden from pickers.',
                          )
                        }
                      >
                        {category.active === false ? 'Activate' : 'Deactivate'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </Card>
      )}
    </AdminPageShell>
  );
}
