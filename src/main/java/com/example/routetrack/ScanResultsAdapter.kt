package com.example.routetrack

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView


class ScanResultsAdapter(private var items: List<Invoice>, private var onDeleteClickListener: ((Invoice) -> Unit)? = null) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val TYPE_SCAN_RESULT = 0
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            else -> TYPE_SCAN_RESULT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SCAN_RESULT -> {
                val view = inflater.inflate(R.layout.item_scan_result, parent, false)
                ScanResultViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ScanResultViewHolder -> {
                val result = items[position] as Invoice
                holder.bind(result)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // Метод для обновления данных в адаптере

    // ViewHolder для ScanResult
    inner class ScanResultViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val invoiceNumberEditText: EditText = view.findViewById(R.id.invoiceNumberEditText)
        private val amountEditText: TextView = view.findViewById(R.id.amountEditText)
        private val afterDeliveryAmountEditText: TextView = view.findViewById(R.id.afterDeliveryAmountEditText)
        private val discountAmountEditText: EditText = view.findViewById(R.id.discountAmountEditText)
        private val paymentOptionsSpinner: Spinner = view.findViewById(R.id.paymentOptionsSpinner)
        private val selectedPaymentTextView: TextView = view.findViewById(R.id.selectedPaymentTextView)
        private val dateTextView: TextView = view.findViewById(R.id.dateTextView)
        private val deliveryStatusCheckBox: CheckBox = view.findViewById(R.id.deliveryStatusCheckBox)

        init {
            // Устанавливаем слушатель длительного нажатия
            itemView.setOnLongClickListener {
                val position = bindingAdapterPosition // Используем bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClickListener?.invoke(items[position])
                }
                true // Возвращаем true, чтобы указать, что событие обработано
            }

            deliveryStatusCheckBox.setOnCheckedChangeListener{ _, isChecked ->
                
            }
        }

        fun bind(result: Invoice) {
            // Заполняем поля
            dateTextView.text = result.date
            invoiceNumberEditText.setText(result.number)
            amountEditText.setText(result.amount.toString())
            afterDeliveryAmountEditText.setText(result.afterDeliveryAmount.toString())
            discountAmountEditText.setText(result.discount.toString())

            // Подсветка
            if (result.isEditable) {
                itemView.setBackgroundColor(Color.LTGRAY) // Цвет выделенного элемента
            } else {
                itemView.setBackgroundColor(Color.WHITE) // Стандартный цвет
            }

            // Настройка Spinner
            val context = itemView.context
            val paymentOptions = context.resources.getStringArray(R.array.payment_options)
            val spinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, paymentOptions)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            paymentOptionsSpinner.adapter = spinnerAdapter

            // Установить выбранный элемент
            val selectedPosition = paymentOptions.indexOf(result.result)
            if (selectedPosition >= 0) {
                paymentOptionsSpinner.setSelection(selectedPosition, false)
            }

            // Установить текст выбранного способа в TextView
            selectedPaymentTextView.text = result.result.ifEmpty { "Не выбран" }

            // Слушатель выбора
            paymentOptionsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    result.result = paymentOptions[position] // Сохраняем выбранное значение
                    selectedPaymentTextView.text = paymentOptions[position] // Обновляем текст выбранного
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    result.result = "" // Устанавливаем значение по умолчанию
                }
            }

            // Слушатели для других полей
            invoiceNumberEditText.addTextChangedListener { editable ->
                result.number = editable?.toString() ?: ""
            }
            amountEditText.addTextChangedListener { editable ->
                result.amount = editable?.toString()?.toDoubleOrNull() ?: 0.0
            }
            afterDeliveryAmountEditText.addTextChangedListener { editable ->
                result.afterDeliveryAmount = editable?.toString()?.toDoubleOrNull() ?: 0.0
            }
            discountAmountEditText.addTextChangedListener { editable ->
                result.discount = editable?.toString()?.toDoubleOrNull() ?: 0.0
            }

            if (result.isDelivered) {
                itemView.setBackgroundColor(Color.parseColor("#D3D3D3")) // Светло-серый для доставленных
            } else {
                itemView.setBackgroundColor(Color.WHITE) // Белый для недоставленных
            }
        }
    }

    data class Invoice(
        val date: String, // Дата накладной
        var number: String, // Номер накладной
        var amount: Double, // Сумма
        val amountBefore: Double?, // Сумма до доставки (опционально)
        var discount: Double, // Скидка
        var result: String, // Результат/статус
        val isEditable: Boolean = false, // Определяет, редактируемый ли элемент
        var afterDeliveryAmount: Double?, // Сумма после доставки (опционально)
        var selected: Boolean = false,
        val data: List<Any>? = null,
        val isDelivered: Boolean = false
    )

    fun updateItems(newItems: List<Invoice>) {
        items = newItems.sortedBy { it.isDelivered }
        notifyDataSetChanged()
    }

}



