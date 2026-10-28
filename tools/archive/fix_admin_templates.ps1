$files = @('admin-advertisements.html', 'admin-advertisement-form.html', 'admin-advertisement-details.html')
foreach ($f in $files) {
    $path = "src\main\resources\templates\$f"
    $content = Get-Content $path -Raw
    $content = $content.Replace("`${users.size()}", "`${users != null ? users.size() : 0}")
    $content = $content.Replace("`${posts.size()}", "`${posts != null ? posts.size() : 0}")
    Set-Content -Path $path -Value $content
}
