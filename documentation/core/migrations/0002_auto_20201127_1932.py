# Generated by Django 3.1.3 on 2020-11-27 19:32

from django.db import migrations, models
import django.db.models.deletion


class Migration(migrations.Migration):

    dependencies = [
        ('core', '0001_initial'),
    ]

    operations = [
        migrations.AlterField(
            model_name='fhirprofile',
            name='mapping',
            field=models.OneToOneField(default=None, null=True, on_delete=django.db.models.deletion.CASCADE, related_name='profile', to='core.mapping'),
        ),
        migrations.AlterField(
            model_name='templateclass',
            name='mapping',
            field=models.OneToOneField(default=None, null=True, on_delete=django.db.models.deletion.CASCADE, related_name='template', to='core.mapping'),
        ),
    ]
