/***************************************************************************
 *   CoolReader, Qt GUI                                                    *
 *   Copyright (C) 2009,2012 Vadim Lopatin <coolreader.org@gmail.com>      *
 *   Copyright (C) 2020,2021 Aleksey Chernov <valexlin@gmail.com>          *
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

#include "crqtutil.h"
#include "../crengine/include/props.h"
#include "../crengine/include/crlocaledata.h"
#include <QStringList>
#include <QWidget>
#include <QPoint>

lString32 qt2cr(QString str)
{
    return lString32( str.toUtf8().constData() );
}

QString cr2qt(lString32 str)
{
    return QString::fromUcs4(str.c_str(), str.length());
}

class CRPropsImpl : public Props
{
    CRPropRef _ref;
public:
    explicit CRPropsImpl(CRPropRef ref) : _ref( ref ) { }
    int count() override { return _ref->getCount(); }
    const char * name( int index ) override { return _ref->getName( index ); }
    QString value( int index ) override { return cr2qt(_ref->getValue( index )); }
    bool hasProperty( const char * propName ) const override { return _ref->hasProperty(propName); }
    bool getString( const char * prop, QString & result ) override
    {
        lString32 value;
        if ( !_ref->getString(prop, value) )
            return false;
        result = cr2qt( value );
        return true;
    }
    QString getStringDef( const char * prop, const char * defValue ) override
    {
        return cr2qt( _ref->getStringDef(prop, defValue) );
    }
    void setString( const char * prop, const QString & value ) override
    {
        _ref->setString( prop, qt2cr(value) );
    }
    bool getInt( const char * prop, int & result ) override
    {
        return _ref->getInt(prop, result);
    }
    void setInt( const char * prop, int value ) override
    {
        _ref->setInt( prop, value );
    }
    int getIntDef( const char * prop, int defValue ) override
    {
        return _ref->getIntDef(prop, defValue);
    }
    unsigned getColorDef( const char * prop, unsigned defValue ) override
    {
        return _ref->getColorDef(prop, defValue);
    }
    bool getBoolDef( const char * prop, bool defValue ) override
    {
        return _ref->getBoolDef(prop, defValue);
    }

    void setHex( const char * propName, int value ) override
    {
        _ref->setHex( propName, value );
    }
    const CRPropRef & accessor() override
    {
        return _ref;
    }
    ~CRPropsImpl() override = default;
};

static PropsRef makeProps(CRPropRef ref)
{
#if QT_VERSION >= 0x050100
    return QSharedPointer<CRPropsImpl>::create(ref);
#else
    return QSharedPointer<Props>(new CRPropsImpl(ref));
#endif
}

PropsRef cr2qt( CRPropRef & ref )
{
    return makeProps(ref);
}

const CRPropRef & qt2cr( PropsRef & ref )
{
    return ref->accessor();
}


PropsRef Props::create()
{
    return makeProps(LVCreatePropsContainer());
}

PropsRef Props::clone( PropsRef v )
{
    return makeProps(LVClonePropsContainer(v->accessor()));
}

/// returns common items from props1 not containing in props2
PropsRef operator - ( PropsRef props1, PropsRef props2 )
{
    return makeProps(props1->accessor() - props2->accessor());
}

/// returns common items containing in props1 or props2
PropsRef operator | ( PropsRef props1, PropsRef props2 )
{
    return makeProps(props1->accessor() | props2->accessor());
}

/// returns common items of props1 and props2
PropsRef operator & ( PropsRef props1, PropsRef props2 )
{
    return makeProps(props1->accessor() & props2->accessor());
}

/// returns added or changed items of props2 compared to props1
PropsRef operator ^ ( PropsRef props1, PropsRef props2 )
{
    return makeProps(props1->accessor() ^ props2->accessor());
}

void cr2qt( QStringList & dst, const lString32Collection & src )
{
    dst.clear();
    for ( int i=0; i<src.length(); i++ ) {
        dst.append( cr2qt( src[i] ) );
    }
}

void qt2cr( lString32Collection & dst, const QStringList & src )
{
    dst.clear();
    for ( int i=0; i<src.length(); i++ ) {
        dst.add( qt2cr( src[i] ) );
    }
}

void crGetFontFaceList( QStringList & dst )
{
    lString32Collection faceList;
    fontMan->getFaceList( faceList );
    cr2qt( dst, faceList );
}

QString getHumanReadableLocaleName(lString32 langTag)
{
#if USE_LOCALE_DATA==1
    QString res;
    CRLocaleData loc(UnicodeToUtf8(langTag));
    if (loc.isValid()) {
        res = loc.langName().c_str();
        if (loc.scriptNumeric() > 0) {
            res.append("-");
            res.append(loc.scriptName().c_str());
        }
        if (loc.regionNumeric() > 0) {
            res.append(" (");
            res.append(loc.regionAlpha3().c_str());
            res.append(")");
        }
    }
#else
    QString res = QT_TRANSLATE_NOOP("crqtutils", "Undetermined");
#endif
    return res;
}

QString crpercent( int p )
{
    return QString("%1.%2%").arg(p/100).arg(p%100,2, 10,QLatin1Char('0'));
}

/// save window position to properties
void saveWindowPosition( QWidget * window, CRPropRef props, const char * prefix )
{
    QPoint pos = window->pos();
    QSize size = window->size();
    bool minimized = window->isMinimized();
    bool maximized = window->isMaximized();
    bool fs = window->isFullScreen();
    CRPropRef p = props->getSubProps( prefix );
    p->setBool( "window.minimized", minimized );
    p->setBool( "window.maximized", maximized );
    p->setBool( "window.fullscreen", fs );
    if ( !minimized && !maximized && !fs ) {
        p->setPoint( "window.pos", lvPoint( pos.x(), pos.y() ) );
        p->setPoint( "window.size", lvPoint( size.width(), size.height() ) );
    }
}

/// restore window position from properties
void restoreWindowPosition( QWidget * window, CRPropRef props, const char * prefix, bool allowFullscreen )
{
    CRPropRef p = props->getSubProps( prefix );
    lvPoint pos;
    bool posRead = p->getPoint( "window.pos", pos );
    lvPoint size;
    bool sizeRead = p->getPoint( "window.size", size );

    if ( posRead && sizeRead ) {
        if ( size.x > 100 && size.y>100 ) {
            window->resize( size.x, size.y );
            window->move( pos.x, pos.y );
        }
        //window->setGeometry( pos.x, pos.y, size.x, size.y );
    }
    if ( allowFullscreen ) {
        bool minimized = p->getBoolDef( "window.minimized", false );
        bool maximized = p->getBoolDef( "window.maximized", false );
        bool fs = p->getBoolDef( "window.fullscreen", false );
        if ( fs ) {
            window->showFullScreen ();
        } else if ( maximized ) {
            window->showMaximized();
        } else if ( minimized ) {
            window->showMinimized();
        }
    }
}
