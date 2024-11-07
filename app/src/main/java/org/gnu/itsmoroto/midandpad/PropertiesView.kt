package org.gnu.itsmoroto.midandpad

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout

abstract class PropertiesView (context: Context): ConstraintLayout(context){
    abstract fun setProps (btn: EventButton)
    abstract fun getProps (btn: EventButton): Boolean
}