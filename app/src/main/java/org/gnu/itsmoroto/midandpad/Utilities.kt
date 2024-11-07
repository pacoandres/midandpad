package org.gnu.itsmoroto.midandpad

import android.content.Context
import android.text.Editable
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton.inflate

interface TypeLabel {
    fun getLabelId ():Int
}

class TypeLabelAdapter<T:TypeLabel> (context: Context, resid: Int, objects: Array<T>):
    ArrayAdapter<T>(context, resid, objects){
    private val mLayoutId = resid
    private var mDropDownLayoutId: Int = 0
    override fun setDropDownViewResource(resource: Int) {
        mDropDownLayoutId = resource
        super.setDropDownViewResource(resource)
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textfield = inflate (context, mLayoutId,null) as TextView
        if (position > -1 && position < count)
            textfield.setText(getItem(position)!!.getLabelId())
        return textfield
        //return super.getView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val textfield = inflate (context, mDropDownLayoutId,null) as TextView
        if (position > -1 && position < count)
            textfield.setText(getItem(position)!!.getLabelId())
        return textfield
        //return super.getDropDownView(position, convertView, parent)
    }
}

class TypeNoteAdapter (context: Context, objects: Array<MidiHelper.NOTE_TIME>):
        ArrayAdapter<MidiHelper.NOTE_TIME> (context, 0, objects){

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val image = inflate (context, R.layout.figureview,null) as ImageView
        if (position > -1 && position < count)
            image.setImageResource(getItem(position)!!.imageId)
        return image
        //return super.getView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val image = inflate (context, R.layout.figureview,null) as ImageView
        if (position > -1 && position < count)
            image.setImageResource(getItem(position)!!.imageId)
        image.minimumHeight = image.height + 10
        return image
        //return super.getDropDownView(position, convertView, parent)
    }
        }

class ReplaceLabels {
    companion object {
        const val FIELDLABEL = "[field]"
        const val VALUELABEL = "[value]"
        const val PRESETNAMELABEL = "[presetname]"
    }
}


fun String.toEditable() : Editable =
    Editable.Factory.getInstance().newEditable(this)

fun Int.toEditable (): Editable =
    Editable.Factory.getInstance().newEditable(this.toString())