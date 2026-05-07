# Gestão de Projeto Atômica — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Tornar a tela `GestaoProjetoPanel` atômica — projeto e tarefas são persistidos juntos em um único Salvar no rodapé.

**Architecture:** Três coleções em memória rastreiam mudanças pendentes (`tarefasNovas`, `tarefasEditadas`, `tarefasExcluidas`). Todas as operações de tarefa alimentam o staging em vez do banco. Um único SwingWorker persiste tudo no Salvar. Um botão Cancelar e um aviso de "alterações não salvas" ao fechar a janela completam o fluxo.

**Tech Stack:** Java 21, Swing, Maven. Sem framework de testes (projeto sem src/test). Verificação via `mvn compile`. Execução manual para testar.

---

## Mapa de Arquivos

| Arquivo | Ação | Responsabilidade |
|---|---|---|
| `src/main/java/com/vbaggio/projectapp/view/DadosTarefa.java` | **Criar** | Record com os campos editáveis de uma tarefa |
| `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java` | **Modificar** | Staging, layout, diálogos, save/cancel atômicos |
| `src/main/java/com/vbaggio/projectapp/view/ProjetoPanel.java` | **Modificar** | Aviso de alterações não salvas ao fechar a JFrame |

---

## Task 1: Criar DadosTarefa e adicionar campos de staging

**Files:**
- Create: `src/main/java/com/vbaggio/projectapp/view/DadosTarefa.java`
- Modify: `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java`

- [ ] **Step 1.1: Criar o record DadosTarefa**

```java
// src/main/java/com/vbaggio/projectapp/view/DadosTarefa.java
package com.vbaggio.projectapp.view;

import com.vbaggio.projectapp.model.enums.StatusTarefa;
import java.time.LocalDate;
import java.util.UUID;

record DadosTarefa(String nome, String descricao, LocalDate prazo,
                   UUID responsavelId, StatusTarefa status) {}
```

- [ ] **Step 1.2: Adicionar imports e campos de staging em GestaoProjetoPanel**

Nos imports do arquivo, adicionar:
```java
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
```

Após a declaração `private final JLabel lblContagem`, adicionar os três campos de staging e o método `temAlteracoesPendentes`:

```java
// Staging — changes not yet persisted
private final Map<UUID, DadosTarefa> tarefasNovas     = new LinkedHashMap<>();
private final Map<UUID, DadosTarefa> tarefasEditadas  = new LinkedHashMap<>();
private final Set<UUID>              tarefasExcluidas  = new LinkedHashSet<>();

public boolean temAlteracoesPendentes() {
    return !tarefasNovas.isEmpty() || !tarefasEditadas.isEmpty() || !tarefasExcluidas.isEmpty();
}
```

- [ ] **Step 1.3: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS, sem erros.

- [ ] **Step 1.4: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/DadosTarefa.java \
        src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java
git commit -m "feat(gestao): add DadosTarefa record and staging fields"
```

---

## Task 2: Refatorar layout — remover Salvar do meio, adicionar rodapé

**Files:**
- Modify: `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java`

- [ ] **Step 2.1: Remover botão Salvar de criarBlocoProjet()**

Substituir o método `criarBlocoProjet()` inteiro por:

```java
private JPanel criarBlocoProjet() {
    campDesc.setLineWrap(true); campDesc.setWrapStyleWord(true);

    JPanel form = montarForm(
            "Nome:",                  campNome,
            "Descrição:",             new JScrollPane(campDesc),
            "Status:",                comboStatus,
            "Gerente:",               comboGerente,
            "Início (dd/MM/yyyy):",   campInicio,
            "Previsão (dd/MM/yyyy):", campPrevisao);

    JPanel bloco = new JPanel(new BorderLayout(0, 4));
    bloco.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
    bloco.add(form, BorderLayout.CENTER);
    return bloco;
}
```

- [ ] **Step 2.2: Adicionar método criarRodape()**

Adicionar após o método `criarBlocoTarefas()`:

```java
private JPanel criarRodape() {
    JButton btnCancelar = new JButton("Cancelar");
    JButton btnSalvar   = new JButton("Salvar");
    btnCancelar.addActionListener(e -> cancelar());
    btnSalvar.addActionListener(e -> salvarTudo());

    JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
    direita.add(btnCancelar);
    direita.add(btnSalvar);

    JPanel rodape = new JPanel(new BorderLayout());
    rodape.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0,
                    UIManager.getColor("Separator.foreground")),
            BorderFactory.createEmptyBorder(6, 8, 8, 8)));
    rodape.add(direita, BorderLayout.EAST);
    return rodape;
}
```

- [ ] **Step 2.3: Atualizar construtor para adicionar rodapé SOUTH**

Localizar o construtor e adicionar a linha do rodapé:

```java
public GestaoProjetoPanel(UUID projetoId, Runnable onSalvar) {
    this.projetoId = projetoId;
    this.onSalvar  = onSalvar;
    setLayout(new BorderLayout(0, 0));
    add(criarBlocoProjet(),  BorderLayout.NORTH);
    add(criarBlocoTarefas(), BorderLayout.CENTER);
    add(criarRodape(),       BorderLayout.SOUTH);
    configurarTabela();
    carregarProjeto();
    carregarTarefas();
}
```

- [ ] **Step 2.4: Estender DefaultTableModel para 7 colunas (adicionar RespID e Desc ocultas)**

Substituir a declaração `modeloTarefas`:

```java
private final DefaultTableModel modeloTarefas = new DefaultTableModel(
        new String[]{"ID", "Nome", "Status", "Prazo", "Responsável", "RespID", "Desc"}, 0) {
    @Override public boolean isCellEditable(int r, int c) { return c == 2; }
};
```

- [ ] **Step 2.5: Ocultar colunas 5 e 6 em configurarTabela()**

Ao final do bloco que oculta a coluna 0, adicionar:

```java
tabelaTarefas.getColumnModel().getColumn(5).setMinWidth(0);
tabelaTarefas.getColumnModel().getColumn(5).setMaxWidth(0);
tabelaTarefas.getColumnModel().getColumn(5).setWidth(0);
tabelaTarefas.getColumnModel().getColumn(6).setMinWidth(0);
tabelaTarefas.getColumnModel().getColumn(6).setMaxWidth(0);
tabelaTarefas.getColumnModel().getColumn(6).setWidth(0);
```

- [ ] **Step 2.6: Atualizar carregarTarefas() para preencher 7 colunas**

Na chamada `modeloTarefas.addRow(...)` dentro do `done()` de `carregarTarefas()`, substituir por:

```java
modeloTarefas.addRow(new Object[]{
    t.getId().toString(),
    t.getNome(),
    t.getStatus(),
    DateUtils.format(t.getPrazo()),
    t.getResponsavel() != null ? t.getResponsavel().getNome() : "",
    t.getResponsavel() != null ? t.getResponsavel().getId().toString() : "",
    t.getDescricao() != null ? t.getDescricao() : ""
});
```

- [ ] **Step 2.7: Extrair atualizarContagem() para uso pelo staging**

Substituir o bloco `lblContagem.setText(...)` dentro do `done()` de `carregarTarefas()` por uma chamada ao novo método:

```java
atualizarContagem();
```

E adicionar o método após `carregarTarefas()`:

```java
private void atualizarContagem() {
    int total = modeloTarefas.getRowCount();
    long concluidas = 0;
    for (int i = 0; i < total; i++) {
        if (modeloTarefas.getValueAt(i, 2) == StatusTarefa.CONCLUIDA) concluidas++;
    }
    lblContagem.setText("Tarefas (" + total + " total · "
            + concluidas + " concluída" + (concluidas != 1 ? "s" : "") + ")");
}
```

- [ ] **Step 2.8: Adicionar stubs salvarTudo() e cancelar() para compilar**

Adicionar temporariamente após `atualizarContagem()`:

```java
private void salvarTudo() { /* Task 7 */ }
private void cancelar()   { /* Task 8 */ }
```

- [ ] **Step 2.9: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 2.10: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java
git commit -m "feat(gestao): rodape com Salvar/Cancelar; tabela com 7 colunas"
```

---

## Task 3: Refatorar novaTarefa() — staging + campo Status

**Files:**
- Modify: `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java`

- [ ] **Step 3.1: Substituir novaTarefa() inteiro**

```java
private void novaTarefa() {
    JTextField          campNomeTarefa = new JTextField(24);
    JTextArea           campDescTarefa = new JTextArea(3, 24);
    campDescTarefa.setLineWrap(true); campDescTarefa.setWrapStyleWord(true);
    JFormattedTextField campPrazo      = DateUtils.campData();
    JComboBox<OpcaoItem>    comboResp  = montarComboResponsavel();
    JComboBox<StatusTarefa> comboSt    = new JComboBox<>();
    comboSt.addItem(StatusTarefa.PENDENTE);
    for (StatusTarefa prox : StatusTarefa.PENDENTE.proximosStatus()) comboSt.addItem(prox);
    comboSt.setSelectedItem(StatusTarefa.PENDENTE);

    JPanel form = montarForm(
            "Nome:",              campNomeTarefa,
            "Descrição:",         new JScrollPane(campDescTarefa),
            "Prazo (dd/MM/yyyy):", campPrazo,
            "Responsável:",       comboResp,
            "Status:",            comboSt);

    while (true) {
        int op = JOptionPane.showConfirmDialog(this, form, "Nova Tarefa",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;
        String nome = campNomeTarefa.getText().trim();
        if (nome.isBlank()) {
            JOptionPane.showMessageDialog(this, "Nome da tarefa é obrigatório.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            continue;
        }
        UUID      tempId = UUID.randomUUID();
        OpcaoItem resp   = (OpcaoItem) comboResp.getSelectedItem();
        DadosTarefa dados = new DadosTarefa(
                nome,
                campDescTarefa.getText().trim(),
                DateUtils.parse(campPrazo.getText()),
                resp != null ? resp.id() : null,
                (StatusTarefa) comboSt.getSelectedItem());
        tarefasNovas.put(tempId, dados);
        modeloTarefas.addRow(new Object[]{
                tempId.toString(),
                dados.nome(),
                dados.status(),
                DateUtils.format(dados.prazo()),
                resp != null && resp.id() != null ? resp.label() : "",
                resp != null && resp.id() != null ? resp.id().toString() : "",
                dados.descricao() != null ? dados.descricao() : ""
        });
        atualizarContagem();
        return;
    }
}
```

- [ ] **Step 3.2: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 3.3: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java
git commit -m "feat(gestao): novaTarefa usa staging; campo Status no dialogo"
```

---

## Task 4: Refatorar editarTarefa() — staging + campo Status

**Files:**
- Modify: `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java`

- [ ] **Step 4.1: Adicionar helper dadosParaStaging()**

Adicionar após `atualizarContagem()`:

```java
private DadosTarefa dadosParaStaging(UUID uuid, int row) {
    if (tarefasNovas.containsKey(uuid))    return tarefasNovas.get(uuid);
    if (tarefasEditadas.containsKey(uuid)) return tarefasEditadas.get(uuid);
    Object respIdObj = modeloTarefas.getValueAt(row, 5);
    UUID   respId    = (respIdObj == null || respIdObj.toString().isEmpty())
                       ? null : UUID.fromString(respIdObj.toString());
    Object descObj   = modeloTarefas.getValueAt(row, 6);
    String desc      = descObj != null ? descObj.toString() : null;
    return new DadosTarefa(
            modeloTarefas.getValueAt(row, 1).toString(),
            desc,
            DateUtils.parse(modeloTarefas.getValueAt(row, 3).toString()),
            respId,
            (StatusTarefa) modeloTarefas.getValueAt(row, 2));
}
```

- [ ] **Step 4.2: Substituir editarTarefa() inteiro**

```java
private void editarTarefa() {
    int linha = tabelaTarefas.getSelectedRow();
    if (linha < 0) return;
    UUID uuid = UUID.fromString((String) modeloTarefas.getValueAt(linha, 0));

    DadosTarefa base = dadosParaStaging(uuid, linha);

    JTextField          campNomeTarefa = new JTextField(base.nome(), 24);
    JTextArea           campDescTarefa = new JTextArea(
            base.descricao() != null ? base.descricao() : "", 3, 24);
    campDescTarefa.setLineWrap(true); campDescTarefa.setWrapStyleWord(true);
    JFormattedTextField campPrazo = DateUtils.campData();
    if (base.prazo() != null) campPrazo.setText(DateUtils.format(base.prazo()));

    JComboBox<OpcaoItem> comboResp = montarComboResponsavel();
    if (base.responsavelId() != null) {
        for (int i = 0; i < comboResp.getItemCount(); i++) {
            if (comboResp.getItemAt(i).id() != null
                    && comboResp.getItemAt(i).id().equals(base.responsavelId())) {
                comboResp.setSelectedIndex(i); break;
            }
        }
    }

    JComboBox<StatusTarefa> comboSt = new JComboBox<>();
    comboSt.addItem(base.status());
    for (StatusTarefa prox : base.status().proximosStatus()) comboSt.addItem(prox);
    comboSt.setSelectedItem(base.status());

    JPanel form = montarForm(
            "Nome:",              campNomeTarefa,
            "Descrição:",         new JScrollPane(campDescTarefa),
            "Prazo (dd/MM/yyyy):", campPrazo,
            "Responsável:",       comboResp,
            "Status:",            comboSt);

    while (true) {
        int op = JOptionPane.showConfirmDialog(this, form, "Editar Tarefa",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (op != JOptionPane.OK_OPTION) return;
        String nome = campNomeTarefa.getText().trim();
        if (nome.isBlank()) {
            JOptionPane.showMessageDialog(this, "Nome da tarefa é obrigatório.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            continue;
        }
        OpcaoItem resp = (OpcaoItem) comboResp.getSelectedItem();
        DadosTarefa dados = new DadosTarefa(
                nome,
                campDescTarefa.getText().trim(),
                DateUtils.parse(campPrazo.getText()),
                resp != null ? resp.id() : null,
                (StatusTarefa) comboSt.getSelectedItem());

        if (tarefasNovas.containsKey(uuid)) {
            tarefasNovas.put(uuid, dados);
        } else {
            tarefasEditadas.put(uuid, dados);
        }

        modeloTarefas.setValueAt(dados.nome(),   linha, 1);
        modeloTarefas.setValueAt(dados.status(), linha, 2);
        modeloTarefas.setValueAt(DateUtils.format(dados.prazo()), linha, 3);
        modeloTarefas.setValueAt(resp != null && resp.id() != null ? resp.label() : "", linha, 4);
        modeloTarefas.setValueAt(resp != null && resp.id() != null ? resp.id().toString() : "", linha, 5);
        modeloTarefas.setValueAt(dados.descricao() != null ? dados.descricao() : "", linha, 6);
        atualizarContagem();
        return;
    }
}
```

- [ ] **Step 4.3: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 4.4: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java
git commit -m "feat(gestao): editarTarefa usa staging; campo Status no dialogo"
```

---

## Task 5: Refatorar excluirTarefa() — staging

**Files:**
- Modify: `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java`

- [ ] **Step 5.1: Substituir excluirTarefa() inteiro**

```java
private void excluirTarefa() {
    int linha = tabelaTarefas.getSelectedRow();
    if (linha < 0) return;
    UUID uuid = UUID.fromString(modeloTarefas.getValueAt(linha, 0).toString());
    String nome = modeloTarefas.getValueAt(linha, 1).toString();

    int conf = JOptionPane.showConfirmDialog(this,
            "Excluir a tarefa '" + nome + "'?",
            "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
    if (conf != JOptionPane.YES_OPTION) return;

    if (tarefasNovas.containsKey(uuid)) {
        tarefasNovas.remove(uuid);
    } else {
        StatusTarefa status = (StatusTarefa) modeloTarefas.getValueAt(linha, 2);
        if (status == StatusTarefa.EM_ANDAMENTO || status == StatusTarefa.CONCLUIDA) {
            JOptionPane.showMessageDialog(this,
                    "Apenas tarefas PENDENTE ou CANCELADA podem ser removidas.",
                    "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }
        tarefasEditadas.remove(uuid);
        tarefasExcluidas.add(uuid);
    }

    modeloTarefas.removeRow(linha);
    atualizarContagem();
}
```

- [ ] **Step 5.2: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 5.3: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java
git commit -m "feat(gestao): excluirTarefa usa staging"
```

---

## Task 6: Refatorar editor inline de status — staging

**Files:**
- Modify: `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java`

- [ ] **Step 6.1: Substituir stopCellEditing() dentro de configurarTabela()**

Localizar o `new DefaultCellEditor(...)` em `configurarTabela()` e substituir apenas o método `stopCellEditing()` por:

```java
@Override
public boolean stopCellEditing() {
    StatusTarefa novo = (StatusTarefa) getCellEditorValue();
    int row = tabelaTarefas.getEditingRow();
    if (row >= 0) {
        UUID uuid = UUID.fromString((String) modeloTarefas.getValueAt(row, 0));
        StatusTarefa atual = (StatusTarefa) modeloTarefas.getValueAt(row, 2);
        if (novo != atual) {
            super.stopCellEditing();
            DadosTarefa base = dadosParaStaging(uuid, row);
            DadosTarefa atualizado = new DadosTarefa(
                    base.nome(), base.descricao(), base.prazo(),
                    base.responsavelId(), novo);
            if (tarefasNovas.containsKey(uuid)) {
                tarefasNovas.put(uuid, atualizado);
            } else {
                tarefasEditadas.put(uuid, atualizado);
            }
            atualizarContagem();
            return true;
        }
    }
    return super.stopCellEditing();
}
```

Remover também o `carregarTarefas()` que existia dentro do `done()` do SwingWorker antigo (o SwingWorker inteiro é removido — não há mais DB call aqui).

- [ ] **Step 6.2: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 6.3: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java
git commit -m "feat(gestao): status inline usa staging (sem I/O no stopCellEditing)"
```

---

## Task 7: Implementar salvarTudo()

**Files:**
- Modify: `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java`

- [ ] **Step 7.1: Remover salvarProjeto() e substituir o stub salvarTudo()**

Remover o método `salvarProjeto()` inteiro. Substituir o stub `salvarTudo()` por:

```java
private void salvarTudo() {
    StatusProjeto novoStatus = (StatusProjeto) comboStatus.getSelectedItem();
    OpcaoItem     gerenteItem = (OpcaoItem) comboGerente.getSelectedItem();
    if (gerenteItem == null) {
        JOptionPane.showMessageDialog(this, "Selecione um gerente.", "Aviso",
                JOptionPane.WARNING_MESSAGE);
        return;
    }
    UUID      gerenteId = gerenteItem.id();
    String    nome      = campNome.getText().trim();
    String    desc      = campDesc.getText().trim();
    LocalDate inicio    = DateUtils.parse(campInicio.getText());
    LocalDate previsao  = DateUtils.parse(campPrevisao.getText());

    final LocalDate[] dataFimHolder = {null};
    if (novoStatus == StatusProjeto.CONCLUIDO) {
        JFormattedTextField campFim = DateUtils.campData();
        campFim.setText(DateUtils.format(LocalDate.now()));
        while (true) {
            int op = JOptionPane.showConfirmDialog(this, campFim,
                    "Data de Encerramento (dd/MM/yyyy)",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (op != JOptionPane.OK_OPTION) return;
            LocalDate dataFim = DateUtils.parse(campFim.getText());
            if (dataFim == null) {
                JOptionPane.showMessageDialog(this, "Informe a data de encerramento.",
                        "Erro", JOptionPane.ERROR_MESSAGE);
                continue;
            }
            dataFimHolder[0] = dataFim;
            break;
        }
    }

    Map<UUID, DadosTarefa> novas    = new LinkedHashMap<>(tarefasNovas);
    Map<UUID, DadosTarefa> editadas = new LinkedHashMap<>(tarefasEditadas);
    Set<UUID>              excluidas = new LinkedHashSet<>(tarefasExcluidas);

    new SwingWorker<Void, Void>() {
        @Override protected Void doInBackground() throws Exception {
            // 1. Projeto
            Projeto atual = projetoCtrl.buscarPorId(projetoId);
            projetoCtrl.atualizarProjeto(projetoId, nome, desc, inicio, previsao, gerenteId);
            if (novoStatus == StatusProjeto.CONCLUIDO
                    && atual.getStatus() != StatusProjeto.CONCLUIDO) {
                projetoCtrl.encerrarProjeto(projetoId, dataFimHolder[0]);
            } else if (novoStatus != atual.getStatus()) {
                projetoCtrl.atualizarStatus(projetoId, novoStatus);
            }
            // 2. Novas tarefas
            for (DadosTarefa d : novas.values()) {
                Tarefa t = tarefaCtrl.criarTarefa(d.nome(), d.descricao(),
                        d.prazo(), projetoId, d.responsavelId());
                if (d.status() != StatusTarefa.PENDENTE) {
                    tarefaCtrl.atualizarStatus(t.getId(), d.status());
                }
            }
            // 3. Tarefas editadas
            for (Map.Entry<UUID, DadosTarefa> e : editadas.entrySet()) {
                UUID id = e.getKey(); DadosTarefa d = e.getValue();
                tarefaCtrl.atualizarTarefa(id, d.nome(), d.descricao(), d.prazo());
                tarefaCtrl.reatribuirResponsavel(id, d.responsavelId());
                Tarefa t = tarefaCtrl.buscarPorId(id).orElseThrow();
                if (t.getStatus() != d.status()) {
                    tarefaCtrl.atualizarStatus(id, d.status());
                }
            }
            // 4. Tarefas excluídas
            for (UUID id : excluidas) {
                tarefaCtrl.removerTarefa(id);
            }
            return null;
        }
        @Override protected void done() {
            try {
                get();
                tarefasNovas.clear();
                tarefasEditadas.clear();
                tarefasExcluidas.clear();
                if (onSalvar != null) onSalvar.run();
                carregarProjeto();
                carregarTarefas();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(GestaoProjetoPanel.this,
                        ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }.execute();
}
```

- [ ] **Step 7.2: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 7.3: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java
git commit -m "feat(gestao): salvarTudo persiste projeto + staging atomicamente"
```

---

## Task 8: Implementar cancelar()

**Files:**
- Modify: `src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java`

- [ ] **Step 8.1: Substituir o stub cancelar() por**

```java
private void cancelar() {
    tarefasNovas.clear();
    tarefasEditadas.clear();
    tarefasExcluidas.clear();
    carregarProjeto();
    carregarTarefas();
}
```

- [ ] **Step 8.2: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 8.3: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/GestaoProjetoPanel.java
git commit -m "feat(gestao): cancelar limpa staging e recarrega do banco"
```

---

## Task 9: Aviso de alterações não salvas ao fechar janela

**Files:**
- Modify: `src/main/java/com/vbaggio/projectapp/view/ProjetoPanel.java`

- [ ] **Step 9.1: Modificar abrirGestao() em ProjetoPanel**

Localizar o método `abrirGestao()` (linha ~144). Substituir inteiro por:

```java
private void abrirGestao() {
    int linha = tabela.getSelectedRow();
    if (linha < 0) return;
    UUID id     = UUID.fromString(modelo.getValueAt(linha, 0).toString());
    String nome = modelo.getValueAt(linha, 1).toString();

    JFrame janela = janelasGestao.get(id);
    if (janela != null && janela.isDisplayable()) {
        janela.toFront(); janela.requestFocus(); return;
    }

    GestaoProjetoPanel gestaoPanel = new GestaoProjetoPanel(id, this::carregar);
    janela = new JFrame("Gerenciar Projeto — " + nome);
    janela.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    janela.setSize(820, 580);
    janela.setMinimumSize(new Dimension(600, 420));
    janela.setLocationRelativeTo(this);
    janela.add(gestaoPanel);

    final UUID   fId        = id;
    final JFrame janelaFinal = janela;
    janela.addWindowListener(new WindowAdapter() {
        @Override public void windowClosing(WindowEvent e) {
            if (gestaoPanel.temAlteracoesPendentes()) {
                int op = JOptionPane.showConfirmDialog(janelaFinal,
                        "Há alterações não salvas. Deseja sair sem salvar?",
                        "Alterações não salvas",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (op != JOptionPane.YES_OPTION) return;
            }
            janelaFinal.dispose();
        }
        @Override public void windowClosed(WindowEvent e) {
            janelasGestao.remove(fId);
            carregar();
        }
    });

    janelasGestao.put(id, janela);
    janela.setVisible(true);
}
```

- [ ] **Step 9.2: Compilar**

```bash
mvn compile -q
```

Esperado: BUILD SUCCESS.

- [ ] **Step 9.3: Commit**

```bash
git add src/main/java/com/vbaggio/projectapp/view/ProjetoPanel.java
git commit -m "feat(gestao): aviso ao fechar janela com alteracoes pendentes"
```

---

## Task 10: Teste manual e verificação

- [ ] **Step 10.1: Compilar e empacotar**

```bash
mvn package -q -DskipTests
```

Esperado: BUILD SUCCESS, `target/projectapp-1.0-SNAPSHOT-jar-with-dependencies.jar` gerado (ou nome equivalente).

- [ ] **Step 10.2: Executar e testar os cenários**

Abrir a tela de Gestão de um projeto e verificar:

1. **Layout** — botão Salvar aparece no rodapé inferior direito; botão Cancelar ao lado.
2. **Nova Tarefa** — diálogo exibe campo Status; ao confirmar, a tarefa aparece na grid sem ir ao banco (verificar: fechar sem salvar — tarefa some).
3. **Editar Tarefa** — diálogo exibe campo Status pré-preenchido com o status atual; edições ficam na grid.
4. **Status inline** — alterar o combo de status na coluna não persiste imediatamente (verificar abrindo outra tela e voltando).
5. **Excluir nova tarefa** — tarefa criada na sessão é removida da grid sem erro.
6. **Excluir tarefa EM_ANDAMENTO** — exibe mensagem de erro e não remove.
7. **Salvar** — persiste projeto + todas as tarefas adicionadas/editadas/excluídas; grid recarrega do banco.
8. **Cancelar** — descarta todas as mudanças e recarrega o estado do banco.
9. **Fechar janela com alterações** — exibe diálogo "Há alterações não salvas. Deseja sair sem salvar?"; escolher Não mantém a janela aberta.
10. **Fechar janela sem alterações** — fecha diretamente sem diálogo.
