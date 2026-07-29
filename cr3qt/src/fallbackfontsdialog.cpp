/***************************************************************************
 *   CoolReader, Qt GUI                                                    *
 *   Copyright (C) 2021 Aleksey Chernov <valexlin@gmail.com>               *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or         *
 *   modify it under the terms of the GNU General Public License           *
 *   as published by the Free Software Foundation; either version 2        *
 *   of the License, or (at your option) any later version.                *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the Free Software           *
 *   Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,            *
 *   MA 02110-1301, USA.                                                   *
 ***************************************************************************/

#include "fallbackfontsdialog.h"
#include "ui_fallbackfontsdialog.h"

#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QComboBox>
#include <QPushButton>
#include <QToolButton>
#include <QSpacerItem>

FallbackFontsDialog::FallbackFontsDialog(QWidget *parent) :
    QDialog(parent),
    ui(new Ui::FallbackFontsDialog)
{
    ui->setupUi(this);
    m_layout = new QVBoxLayout;
    m_spacer = new QSpacerItem(1, 1, QSizePolicy::Minimum, QSizePolicy::Expanding);
    m_layout->addItem(m_spacer);
    ui->frame->setLayout(m_layout);
}

FallbackFontsDialog::FallbackFontsDialog(QWidget* parent, const QStringList& availFaces) :
    QDialog(parent),
    ui(new Ui::FallbackFontsDialog)
{
    ui->setupUi(this);
    m_layout = new QVBoxLayout;
    m_spacer = new QSpacerItem(1, 1, QSizePolicy::Minimum, QSizePolicy::Expanding);
    m_layout->addItem(m_spacer);
    ui->frame->setLayout(m_layout);
    setAvailableFaces(availFaces);
}

FallbackFontsDialog::~FallbackFontsDialog() = default;

void FallbackFontsDialog::setAvailableFaces(const QStringList& availFaces)
{
    m_availableFaces = availFaces;
}

void FallbackFontsDialog::setFallbackFaces(const QString& faces)
{
    m_fallbackFaces = faces;
    QStringList list;
#if (QT_VERSION >= QT_VERSION_CHECK(5, 14, 0))
    list = faces.split(";", Qt::SkipEmptyParts);
#else
    // TODO: for Qt older than 5.14
#endif
    cleanupFontItems();
    m_layout->removeItem(m_spacer);
    QStringList::const_iterator it;
    int item_idx = 0;
    for (it = list.begin(); it != list.end(); ++it) {
        QString face = (*it).trimmed();
        int face_idx = m_availableFaces.indexOf(face);
        if (face_idx >= 0) {
            addFontItem(item_idx, face_idx);
            item_idx++;
        }
    }
    // add empty item
    addFontItem(item_idx, -1);
    m_layout->addItem(m_spacer);
}

void FallbackFontsDialog::slot_delete_clicked()
{
    int item_idx = -1;
    QVariant itemIdxProp;
    QObject* signalSender = sender();
    if (signalSender)
        itemIdxProp = signalSender->property("ITEMIDX");
    if (itemIdxProp.isValid()) {
        bool ok = false;
        int tmp = itemIdxProp.toInt(&ok);
        if (ok)
            item_idx = tmp;
    }
    const int item_count = static_cast<int>(m_items.size());
    if (item_idx >= 0 && item_idx < item_count) {
        bool res = removeFontItem(item_idx);
        if (res) {
            if (!m_items.empty()) {
                FontControlItem* lastItem = m_items.back().get();
                if (lastItem && lastItem->btnDel)
                    lastItem->btnDel->setEnabled(false);
            }
            updateFallbackFaces();
        }
    }
}

void FallbackFontsDialog::slot_currectIndexChanged(int)
{
    int item_idx = -1;
    QVariant itemIdxProp;
    QObject* signalSender = sender();
    if (signalSender)
        itemIdxProp = signalSender->property("ITEMIDX");
    if (itemIdxProp.isValid()) {
        bool ok = false;
        int tmp = itemIdxProp.toInt(&ok);
        if (ok)
            item_idx = tmp;
    }
    const int item_count = static_cast<int>(m_items.size());
    if (item_idx >= 0 && item_idx < item_count) {
        FontControlItem* item = m_items[item_idx].get();
        if (item_idx == item_count - 1) {
            if (item && item->combobox && !item->combobox->currentText().isEmpty()) {
                item->btnDel->setEnabled(true);
                // last empty item changed
                const int new_item_pos = static_cast<int>(m_items.size());
                m_layout->removeItem(m_spacer);
                addFontItem(new_item_pos, -1);
                m_layout->addItem(m_spacer);
            }
        } else if (item_idx == item_count - 2) {
            if (item && item->combobox && item->combobox->currentText().isEmpty()) {
                FontControlItem* lastItem = m_items.back().get();
                if (lastItem && lastItem->combobox && lastItem->combobox->currentText().isEmpty()) {
                    // remove empty last item
                    removeFontItem(item_count - 1);
                    item->btnDel->setEnabled(false);
                }
            }
        }
        updateFallbackFaces();
    }
}

void FallbackFontsDialog::addFontItem(int pos, int face_idx)
{
    std::unique_ptr<FontControlItem> item =
            std::make_unique<FontControlItem>();
    item->row = std::make_unique<QWidget>(ui->frame);
    item->layout = new QHBoxLayout(item->row.get());
    item->layout->setContentsMargins(0, 0, 0, 0);
    item->combobox = new QComboBox(item->row.get());
    item->combobox->setProperty("ITEMIDX", pos);
    item->combobox->addItem("");
    item->combobox->addItems(m_availableFaces);
    if (face_idx >= 0)
        item->combobox->setCurrentIndex(face_idx + 1);
    item->btnDel = new QToolButton(item->row.get());
    item->btnDel->setProperty("ITEMIDX", pos);
    item->btnDel->setEnabled(face_idx >= 0);
    item->btnDel->setIcon(QIcon(":/icons/action/icons/fileclose.png"));
    item->btnDel->setToolTip(tr("Remove this fallback font"));
    item->layout->addWidget(item->combobox, 10);
    item->layout->addWidget(item->btnDel, 0);
    connect(item->btnDel, SIGNAL(clicked()), this, SLOT(slot_delete_clicked()));
    connect(item->combobox, SIGNAL(currentIndexChanged(int)), this, SLOT(slot_currectIndexChanged(int)));
    m_items.push_back(std::move(item));
    m_layout->addWidget(m_items.back()->row.get());
}

bool FallbackFontsDialog::removeFontItem(int pos)
{
    const int item_count = static_cast<int>(m_items.size());
    if (pos >= 0 && pos < item_count) {
        FontControlItem* item = m_items[pos].get();
        if (item && item->row)
            m_layout->removeWidget(item->row.get());
        m_items.erase(m_items.begin() + pos);
        // update item's position
        const int remaining_count = static_cast<int>(m_items.size());
        for (int i = pos; i < remaining_count; i++) {
            FontControlItem* remaining_item = m_items[i].get();
            remaining_item->btnDel->setProperty("ITEMIDX", i);
            remaining_item->combobox->setProperty("ITEMIDX", i);
        }
        return true;
    }
    return false;
}

void FallbackFontsDialog::cleanupFontItems()
{
    for (const std::unique_ptr<FontControlItem>& item : m_items) {
        if (item && item->row)
            m_layout->removeWidget(item->row.get());
    }
    m_items.clear();
}

void FallbackFontsDialog::updateFallbackFaces()
{
    m_fallbackFaces = QString();
    const int item_count = static_cast<int>(m_items.size());
    for (int i = 0; i < item_count - 1; i++) {
        FontControlItem* item = m_items[i].get();
        if (item) {
            if (item->combobox) {
                QString text = item->combobox->currentText();
                if (!text.isEmpty()) {
                    m_fallbackFaces.append(text);
                    if (i < item_count - 2) {
                        m_fallbackFaces.append("; ");
                    }
                }
            }
        }
    }
}
