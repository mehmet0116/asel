package com.aikodasistani.aikodasistani

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

class SqlQueryBuilderActivity : AppCompatActivity() {

    private lateinit var queryTypeSpinner: Spinner
    private lateinit var tableNameInput: EditText
    private lateinit var columnsInput: EditText
    private lateinit var whereInput: EditText
    private lateinit var orderByInput: EditText
    private lateinit var limitInput: EditText
    private lateinit var valuesInput: EditText
    private lateinit var setInput: EditText
    private lateinit var outputText: TextView
    private lateinit var generateButton: Button
    private lateinit var copyButton: Button
    private lateinit var clearButton: Button
    
    // Optional sections
    private lateinit var selectSection: LinearLayout
    private lateinit var insertSection: LinearLayout
    private lateinit var updateSection: LinearLayout

    private val queryTypes = listOf("SELECT", "INSERT", "UPDATE", "DELETE", "CREATE TABLE", "DROP TABLE")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sql_query_builder)

        setupToolbar()
        initViews()
        setupSpinner()
        setupButtons()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.sql_query_builder)
        toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initViews() {
        queryTypeSpinner = findViewById(R.id.queryTypeSpinner)
        tableNameInput = findViewById(R.id.tableNameInput)
        columnsInput = findViewById(R.id.columnsInput)
        whereInput = findViewById(R.id.whereInput)
        orderByInput = findViewById(R.id.orderByInput)
        limitInput = findViewById(R.id.limitInput)
        valuesInput = findViewById(R.id.valuesInput)
        setInput = findViewById(R.id.setInput)
        outputText = findViewById(R.id.outputText)
        generateButton = findViewById(R.id.generateButton)
        copyButton = findViewById(R.id.copyButton)
        clearButton = findViewById(R.id.clearButton)
        
        selectSection = findViewById(R.id.selectSection)
        insertSection = findViewById(R.id.insertSection)
        updateSection = findViewById(R.id.updateSection)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, queryTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        queryTypeSpinner.adapter = adapter

        queryTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateVisibleSections(queryTypes[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateVisibleSections(queryType: String) {
        selectSection.visibility = View.GONE
        insertSection.visibility = View.GONE
        updateSection.visibility = View.GONE

        when (queryType) {
            "SELECT" -> {
                selectSection.visibility = View.VISIBLE
            }
            "INSERT" -> {
                insertSection.visibility = View.VISIBLE
            }
            "UPDATE" -> {
                updateSection.visibility = View.VISIBLE
            }
            "DELETE" -> {
                // Only WHERE is needed
            }
            "CREATE TABLE" -> {
                insertSection.visibility = View.VISIBLE
            }
            "DROP TABLE" -> {
                // Only table name is needed
            }
        }
    }

    private fun setupButtons() {
        generateButton.setOnClickListener {
            generateQuery()
        }

        copyButton.setOnClickListener {
            copyToClipboard()
        }

        clearButton.setOnClickListener {
            clearAll()
        }
    }

    private fun generateQuery() {
        val tableName = tableNameInput.text.toString().trim()
        if (tableName.isEmpty()) {
            Toast.makeText(this, getString(R.string.table_name_required), Toast.LENGTH_SHORT).show()
            return
        }

        val queryType = queryTypeSpinner.selectedItem.toString()
        val query = when (queryType) {
            "SELECT" -> generateSelectQuery(tableName)
            "INSERT" -> generateInsertQuery(tableName)
            "UPDATE" -> generateUpdateQuery(tableName)
            "DELETE" -> generateDeleteQuery(tableName)
            "CREATE TABLE" -> generateCreateTableQuery(tableName)
            "DROP TABLE" -> generateDropTableQuery(tableName)
            else -> ""
        }

        outputText.text = formatQuery(query)
    }

    private fun generateSelectQuery(tableName: String): String {
        val columns = columnsInput.text.toString().trim().ifEmpty { "*" }
        val where = whereInput.text.toString().trim()
        val orderBy = orderByInput.text.toString().trim()
        val limit = limitInput.text.toString().trim()

        val sb = StringBuilder()
        sb.append("SELECT $columns\nFROM $tableName")
        
        if (where.isNotEmpty()) {
            sb.append("\nWHERE $where")
        }
        if (orderBy.isNotEmpty()) {
            sb.append("\nORDER BY $orderBy")
        }
        if (limit.isNotEmpty()) {
            sb.append("\nLIMIT $limit")
        }
        sb.append(";")
        
        return sb.toString()
    }

    private fun generateInsertQuery(tableName: String): String {
        val columns = columnsInput.text.toString().trim()
        val values = valuesInput.text.toString().trim()

        return if (columns.isNotEmpty()) {
            "INSERT INTO $tableName ($columns)\nVALUES ($values);"
        } else {
            "INSERT INTO $tableName\nVALUES ($values);"
        }
    }

    private fun generateUpdateQuery(tableName: String): String {
        val setClause = setInput.text.toString().trim()
        val where = whereInput.text.toString().trim()

        val sb = StringBuilder()
        sb.append("UPDATE $tableName\nSET $setClause")
        
        if (where.isNotEmpty()) {
            sb.append("\nWHERE $where")
        }
        sb.append(";")
        
        return sb.toString()
    }

    private fun generateDeleteQuery(tableName: String): String {
        val where = whereInput.text.toString().trim()

        val sb = StringBuilder()
        sb.append("DELETE FROM $tableName")
        
        if (where.isNotEmpty()) {
            sb.append("\nWHERE $where")
        }
        sb.append(";")
        
        return sb.toString()
    }

    private fun generateCreateTableQuery(tableName: String): String {
        val columns = valuesInput.text.toString().trim()
        return "CREATE TABLE $tableName (\n    $columns\n);"
    }

    private fun generateDropTableQuery(tableName: String): String {
        return "DROP TABLE IF EXISTS $tableName;"
    }

    private fun formatQuery(query: String): String {
        return query.uppercase()
            .replace("SELECT", "SELECT")
            .replace("FROM", "FROM")
            .replace("WHERE", "WHERE")
            .replace("ORDER BY", "ORDER BY")
            .replace("LIMIT", "LIMIT")
            .replace("INSERT INTO", "INSERT INTO")
            .replace("VALUES", "VALUES")
            .replace("UPDATE", "UPDATE")
            .replace("SET", "SET")
            .replace("DELETE FROM", "DELETE FROM")
            .replace("CREATE TABLE", "CREATE TABLE")
            .replace("DROP TABLE", "DROP TABLE")
    }

    private fun copyToClipboard() {
        val text = outputText.text.toString()
        if (text.isNotEmpty()) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("SQL Query", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearAll() {
        tableNameInput.text.clear()
        columnsInput.text.clear()
        whereInput.text.clear()
        orderByInput.text.clear()
        limitInput.text.clear()
        valuesInput.text.clear()
        setInput.text.clear()
        outputText.text = ""
    }
}
